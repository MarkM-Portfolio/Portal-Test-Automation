# AWS Config

terraform {
  backend "s3" {
    bucket = "dx-testarea"
    key    = "terraform-status/dx-core-tests/dx-update-cf-ora21c/dx-update-cf-ora21c-local-ami.key"
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

variable "EXPIRATION_STAMP" {
  type        = string
  description = "Unix Timestamp of expiration date"
}

variable "TEST_EC2_ID" {
  type        = string
  description = "EC2 instance used to get a new AMI from"
}

variable "BUILD_LABEL" {
  type        = string
  description = "Build label of the DX-Core build"
  default     = "no-build-label-provided"
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

