variable "project_id" {
  type = string
}

variable "region" {
  type    = string
  default = "asia-southeast1"
}

variable "name_prefix" {
  type    = string
  default = "risk-engine"
}

variable "subnet_cidr" {
  type    = string
  default = "10.30.0.0/20"
}

variable "postgres_user" {
  type    = string
  default = "risk"
}

variable "postgres_password" {
  type      = string
  sensitive = true
}

variable "k8s_namespace" {
  type    = string
  default = "default"
}

variable "k8s_service_account" {
  type    = string
  default = "risk-engine-ksa"
}
