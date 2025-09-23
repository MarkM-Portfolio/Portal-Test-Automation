# AWS Config

terraform {
  backend "s3" {
    bucket = "dx-testarea"
    region = "us-east-1"
  }
}

variable "AWS_EC2_INSTANCE_TYPE" {
  # "m5.8xlarge"
  # t2.medium
  # t2.mirco
  default     = "t2.large"
  type        = string
  description = "Type of AWS EC2 instance"
}

variable "HOSTED_ZONE" {
  type        = string
  description = "DNS Hosted zone"
  default     = "Z3OEC7SLEHQ2P3"
}

variable "aws_region" {
  default     = "us-east-1"
  type        = string
  description = "AWS region"
}

variable "aws_subnet" {
  default     = "subnet-014047f30974086c8"
  type        = string
  description = "AWS subnet"
}

variable "TEST_RUN_ID" {
  type        = string
  description = "ID of the current run, used for EC2 naming etc."
}

variable "vpc_security_groups" {
  type        = list(string)
  description = "VPC security group list"
  default     = ["sg-0ddaf1862f39be2be"]
}

variable "instance_name" {
  default     = "dx-onpremise"
  type        = string
  description = "Name of the instance to create"
}

variable "instance_owner" {
  default     = "howard.krovetz@hcl.com"
  type        = string
  description = "Owner of the created instance"
}

variable "popo_schedule" {
  default     = "EST-nightly-shutoff-at-8pm"
  type        = string
  description = "POPO Schedule of the created instance"
}

variable "instance_user" {
  default     = "root"
  type        = string
  description = "User of the created instance"
}

variable "domain_suffix" {
  type        = string
  description = "Suffix added to hostname for FQDN"
  default     = ".team-q-dev.com"
}

variable "CF_AMI_ID" {
  type        = string
  description = "ID of update CF Image."
}

variable "DX_CORE_BUILD_VERSION" {
  type        = string
  description = "Build version of DX Core"
}

variable "BUILD_LABEL" {
  type        = string
  description = "Build label of the DX-Core build"
  default     = "no-build-label-provided"
}

variable "use_public_ip" {
  type        = bool
  description = "Allocate and use public IP or not"
  default     = false
}