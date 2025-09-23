data "aws_ami" "centos" {
  owners      = ["461800378586"]
  most_recent = true

  filter {
    name   = "name"
    values = ["CentOS Linux 7 x86_64 HVM *"]
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
