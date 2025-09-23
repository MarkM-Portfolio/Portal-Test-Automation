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
  description = "DNS Zone"
  default     = "/hostedzone/Z3OEC7SLEHQ2P3"
}

variable "aws_region" {
  default     = "us-east-1"
  type        = string
  description = "AWS region"
}

variable "instance_name" {
  default     = "dx-onpremise"
  type        = string
  description = "Name of the instance to create"
}

variable "instance_owner" {
  default     = "jagadishramac.bhagw@hcl.com"
  type        = string
  description = "Owner of the created instance"
}

variable "domain_suffix" {
  type        = string
  description = "Suffix added to hostname for FQDN"
  default     = ".team-q-dev.com"
}
