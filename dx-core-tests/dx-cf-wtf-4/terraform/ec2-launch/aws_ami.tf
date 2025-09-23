data "aws_ami" "dx-update-cf" {
  owners      = ["657641368736"]
  most_recent = true

  filter {
    name    = "tag:Name"
    values  = [var.CF_AMI_ID]
  }
}