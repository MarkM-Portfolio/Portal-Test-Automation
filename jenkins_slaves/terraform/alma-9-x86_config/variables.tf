# AWS Config

terraform {
  backend "s3" {
    bucket = "dx-testarea"
    region = "us-east-1"
  }
}

variable "aws_ec2_instance_type" {
  default     = "m5d.xlarge"
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
  default     = "jenkins-agent"
  type        = string
  description = "Name of the instance to create"
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

variable "ec2_ssh_user" {
  type        = string
  description = "UserID to access EC2 instance via ssh"
  default     = "ec2-user"
}

variable "private_key_file" {
  type        = string
  description = "Private key file name"
  default     = "./test-automation.pem"
}
