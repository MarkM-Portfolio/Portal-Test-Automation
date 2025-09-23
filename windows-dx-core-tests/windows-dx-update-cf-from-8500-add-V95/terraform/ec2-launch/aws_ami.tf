data "aws_ami" "windows-dx-update-cf-from-8500" {
  owners      = ["657641368736"]
  most_recent = true

  filter {
    name    = "tag:Name"
    values  = [var.CF_AMI_ID]
  }
}