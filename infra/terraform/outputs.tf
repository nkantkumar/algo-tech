output "gke_cluster_name" {
  value = google_container_cluster.gke.name
}

output "redis_host" {
  value = google_redis_instance.memorystore.host
}

output "cloudsql_connection_name" {
  value = google_sql_database_instance.postgres.connection_name
}

output "artifact_registry_repo" {
  value = google_artifact_registry_repository.docker_repo.repository_id
}

output "risk_engine_gsa_email" {
  value = google_service_account.risk_engine.email
}
