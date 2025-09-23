# AWS Config

terraform {
  backend "s3" {
    bucket = "dx-testarea"
    region = "us-east-1"
  }
}

variable "aws_ec2_instance_type" {
  default     = "t2.large"
  type        = string
  description = "Type of AWS EC2 instance"
}

variable "hosted_zone" {
  type        = string
  description = "DNS Hosted zone"
  default     = "Z3OEC7SLEHQ2P3"
}

variable "key_name" {
  type        = string
  description = "Resource key name"
  default     = "test-automation-deployments"
}

variable "aws_region" {
  default     = "us-east-1"
  type        = string
  description = "AWS region"
}

variable "aws_subnet" {
  default     = "subnet-00153ed57f803609e"
  type        = string
  description = "AWS subnet (Build02)"
}

variable "vpc_security_groups" {
  type        = list(string)
  description = "VPC security group list"
  default     = ["sg-0ddaf1862f39be2be"]
}

variable "instance_name" {
  default     = "dx-default-terraform"
  type        = string
  description = "Name of the instance to create"
}

variable "instance_owner" {
  default     = "howard.krovetz@hcl.com"
  type        = string
  description = "Owner of the created instance"
}

variable "instance_area" {
  default     = "TEST"
  type        = string
  description = "Area where to create the instance"
}

variable "instance_termflag" {
  default     = "N"
  type        = string
  description = "Value for the termination flag"
}

variable "instance_popo_schedule" {
  default     = ""
  type        = string
  description = "Value for the POPO schedule"
}

variable "instance_savings" {
  default     = ""
  type        = string
  description = "Value for savings info"
}

variable "domain_suffix" {
  type        = string
  description = "Suffix added to hostname for FQDN"
  default     = ".team-q-dev.com"
}

variable "ec2_ssh_user" {
  type        = string
  description = "UserID to access EC2 instance via ssh"
  default     = "centos"
}

variable "private_key_file" {
  type        = string
  description = "Private key file name"
  default     = "./test-automation-deployments.pem"
}

variable "use_public_ip" {
  type        = bool
  description = "Allocate and use public IP or not"
  default     = false
}

variable "instance_DX_SQUAD" {
  default     = ""
  type        = string
  description = "Value for the DX_SQUAD"
}

variable "dedicated_host_id" {
  default     = ""
  type        = string
  description = "Optional ID to assign the EC2 instance to a dedicated host"
}