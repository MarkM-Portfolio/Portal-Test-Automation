# AWS Config

terraform {
  backend "s3" {
    bucket = "dx-testarea"
    region = "us-east-1"
  }
}

variable "aws_region" {
  default     = "us-east-1"
  type        = string
  description = "AWS region"
}

variable "domain_name" {
  type        = string
  description = "Name of the domain to create"
}

variable "record_type" {
  type        = string
  description = "Route53 record type"
}
variable "ip_address" {
  type        = string
  description = "External ip address"
}
variable "hosted_zone" {
  type        = string
  description = "Hosted zone id"
}