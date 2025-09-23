# AWS Config

terraform {
  backend "s3" {
    bucket = "dx-testarea"
    region = "us-east-1"
  }
}

variable "aws_ec2_instance_type" {
  default     = "t2.xlarge"
  type        = string
  description = "Type of AWS EC2 instance"
}

variable "aws_ec2_disk_size" {
  default     = 20
  type        = number
  description = "Size of the EBS volume to create"
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
  default     = "jenkins-master"
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

variable "jenkins_version" {
  type        = string
  description = "Jenkins version to install"
  default     = "centos"
}
