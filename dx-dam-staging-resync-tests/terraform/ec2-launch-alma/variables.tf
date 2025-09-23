# AWS Config

terraform {
  backend "s3" {
    bucket = "dx-testarea"
    key    = "terraform-status/environments/generic-latest.team-q-dev.com/ENVIRONMENT_HOSTNAME.key"
    region = "us-east-1"
  }
}

variable "aws_ec2_instance_type" {
  default     = "t2.large"
  type        = string
  description = "Type of AWS EC2 instance"
}

variable "instance_name" {
  type        = string
  description = "Hostname of the current environment"
  default     = "DX_AT_PIPELINE_DEFAULT"
}

variable "instance_owner" {
  type        = string
  description = "Owner of the EC2 instance"
  default     = "philipp.milich@hcl.com"
}

variable "EC2_SUBNET" {
  type        = string
  description = "Subnet to use for EC2 instance creation"
  default     = "subnet-00153ed57f803609e"
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

variable "EXPIRATION_STAMP" {
  type        = string
  description = "Unix Timestamp of expiration date"
  default     = "1670241413"
}

variable "ec2_ssh_user" {
  type        = string
  description = "UserID to access EC2 instance via ssh"
  default     = "ec2-user"
}

variable "private_key_file" {
  type        = string
  description = "Private key file name"
  default     = "./test-automation-deployments.pem"
}

