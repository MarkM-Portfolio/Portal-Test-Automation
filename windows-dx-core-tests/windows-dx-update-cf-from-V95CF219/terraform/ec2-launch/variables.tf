# AWS Config

terraform {
  backend "s3" {
    bucket = "dx-testarea"
    key    = "terraform-status/windows-dx-core-tests/windows-dx-update-cf-from-V95CF219/windows-dx-update-cf-from-V95CF219-local.key"
    region = "us-east-1"
  }
}

variable "aws_ec2_instance_type" {
  # "m5.8xlarge"
  # t2.medium
  # t2.mirco
  default     = "t2.xlarge"
  type        = string
  description = "Type of AWS EC2 instance"
}

variable "aws_access_key" {
  type        = string
  description = "AWS access key"
}

variable "BASE_AMI_ID" {
  type        = string
  description = "Base AMI ID to be used"
  default     = "ami-06c4a3d1c2abf760e"
}

variable "TEST_RUN_ID" {
  type        = string
  description = "ID of the current run, used for EC2 naming etc."
}

variable "EXPIRATION_STAMP" {
  type        = string
  description = "Unix Timestamp of expiration date"
}

variable "HOSTED_ZONE" {
  type        = string
  description = "DNS Hosted zone"
  default     = "Z3OEC7SLEHQ2P3"
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

