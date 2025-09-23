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

variable "domain_suffix" {
  type        = string
  description = "Suffix added to hostname for FQDN"
  default     = ".team-q-dev.com"
}

variable "use_public_ip" {
  type        = bool
  description = "Allocate and use public IP or not"
  default     = false
}