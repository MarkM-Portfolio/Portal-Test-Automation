# AWS Config

terraform {
  backend "s3" {
    bucket = "dx-testarea"
    key    = "terraform-status/generic-ami/generic-latest.team-q-dev.com/ENVIRONMENT_HOSTNAME.key"
    region = "us-east-1"
  }
}

variable "aws_access_key" {
  type        = string
  description = "AWS access key"
}

variable "TEST_RUN_ID" {
  type        = string
  description = "ID of the current run, used for EC2 naming etc."
}

variable "TEST_EC2_ID" {
  type        = string
  description = "EC2 instance used to get a new AMI from"
}

variable "aws_secret_key" {
  type        = string
  description = "AWS secret"
}

variable "aws_region" {
  default     = "us-east-1"
  type        = string
  description = "AWS region"
}

