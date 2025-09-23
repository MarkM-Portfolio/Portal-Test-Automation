# AWS Config

terraform {
  backend "s3" {
    bucket = "dx-testarea"
    region = "us-east-1"
  }
}

variable "aws_ec2_instance_type" {
  default     = "t3a.large"
  type        = string
  description = "Type of AWS EC2 instance"
}

variable "hosted_zone" {
  type        = string
  description = "DNS Zone"
  default     = "Z3OEC7SLEHQ2P3"
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

variable "aws_region" {
  default     = "us-east-1"
  type        = string
  description = "AWS region"
}

variable "instance_name" {
  default     = "db2-server"
  type        = string
  description = "Name of the instance to create"
}

variable "popo_schedule" {
  default     = "EST-nightly-shutoff-at-8pm"
  type        = string
  description = "POPO Schedule of the created instance"
}

variable "instance_owner" {
  default     = "jenkins"
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
