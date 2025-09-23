# AWS Config

terraform {
  backend "s3" {
    bucket = "dx-testarea"
    key    = "terraform-status/environments/generic-latest.team-q-dev.com/ENVIRONMENT_HOSTNAME.key"
    region = "us-east-1"
  }
}

variable "aws_ec2_instance_type" {
  # "m5.8xlarge"
  # t2.medium
  # t2.mirco
  default     = "t2.large"
  type        = string
  description = "Type of AWS EC2 instance"
}

variable "aws_access_key" {
  type        = string
  description = "AWS access key"
}

variable "ENV_HOSTNAME" {
  type        = string
  description = "Hostname of the current environment"
}

variable "BASE_AMI_ID" {
  type        = string
  description = "Base AMI ID to be used"
  default     = "ami-0e0f3797a664ac96f"
}

variable "EC2_SUBNET" {
  type        = string
  description = "Subnet to use for EC2 instance creation"
  default     = "subnet-033035ecf3a0e7ff4"
}

variable "HOSTED_ZONE" {
  type        = string
  description = "DNS Zone"
  default     = "/hostedzone/Z3OEC7SLEHQ2P3"
}

variable "BUILD_LABEL" {
  type        = string
  description = "Build label of the DX-Core build"
  default     = "no-build-label-provided"
}

variable "EXPIRATION_STAMP" {
  type        = string
  description = "Unix Timestamp of expiration date"
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

