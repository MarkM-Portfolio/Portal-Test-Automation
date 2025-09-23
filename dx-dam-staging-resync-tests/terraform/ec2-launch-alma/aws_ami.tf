data "aws_ami" "alma" {
  owners      = ["764336703387"]
  most_recent = true

  filter {
    name   = "name"
    values = ["AlmaLinux OS 9*"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }
}
