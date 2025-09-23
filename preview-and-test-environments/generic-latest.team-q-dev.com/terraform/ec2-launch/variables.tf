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

variable "EC2_SUBNET" {
  type        = string
  description = "Subnet to use for EC2 instance creation"
  default     = "subnet-014047f30974086c8"
}

variable "HOSTED_ZONE" {
  type        = string
  description = "DNS Zone"
  default     = "/hostedzone/Z3OEC7SLEHQ2P3"
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

