terraform {
  required_version = ">= 1.6.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.40"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

resource "google_project_service" "services" {
  for_each = toset([
    "container.googleapis.com",
    "redis.googleapis.com",
    "sqladmin.googleapis.com",
    "artifactregistry.googleapis.com",
    "iam.googleapis.com",
    "iamcredentials.googleapis.com"
  ])
  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

resource "google_compute_network" "vpc" {
  name                    = "${var.name_prefix}-vpc"
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "subnet" {
  name          = "${var.name_prefix}-subnet"
  ip_cidr_range = var.subnet_cidr
  region        = var.region
  network       = google_compute_network.vpc.id
}

resource "google_container_cluster" "gke" {
  name     = "${var.name_prefix}-autopilot"
  location = var.region
  enable_autopilot = true
  network    = google_compute_network.vpc.id
  subnetwork = google_compute_subnetwork.subnet.id
  depends_on = [google_project_service.services]
}

resource "google_redis_instance" "memorystore" {
  name           = "${var.name_prefix}-redis"
  tier           = "STANDARD_HA"
  memory_size_gb = 1
  region         = var.region
  authorized_network = google_compute_network.vpc.id
  redis_version  = "REDIS_7_0"
  depends_on = [google_project_service.services]
}

resource "google_sql_database_instance" "postgres" {
  name             = "${var.name_prefix}-postgres"
  region           = var.region
  database_version = "POSTGRES_15"
  settings {
    tier = "db-custom-2-7680"
    ip_configuration {
      ipv4_enabled    = false
      private_network = google_compute_network.vpc.id
    }
  }
  deletion_protection = false
  depends_on = [google_project_service.services]
}

resource "google_sql_database" "risk" {
  name     = "risk"
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_user" "risk" {
  name     = var.postgres_user
  instance = google_sql_database_instance.postgres.name
  password = var.postgres_password
}

resource "google_artifact_registry_repository" "docker_repo" {
  location      = var.region
  repository_id = "${var.name_prefix}-images"
  description   = "Docker repo for risk engine"
  format        = "DOCKER"
  depends_on = [google_project_service.services]
}

resource "google_service_account" "risk_engine" {
  account_id   = "risk-engine-gsa"
  display_name = "Risk Engine GSA"
}

resource "google_project_iam_member" "risk_gsa_permissions" {
  for_each = toset([
    "roles/cloudsql.client",
    "roles/redis.viewer",
    "roles/monitoring.metricWriter",
    "roles/cloudtrace.agent"
  ])
  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.risk_engine.email}"
}

resource "google_service_account_iam_member" "workload_identity_binding" {
  service_account_id = google_service_account.risk_engine.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[${var.k8s_namespace}/${var.k8s_service_account}]"
}
