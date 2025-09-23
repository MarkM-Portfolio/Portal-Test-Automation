data "aws_ami" "windowsV95CF204" {
  owners      = ["657641368736"]
  most_recent = true

  filter {
    name    = "image-id"
    values = [var.BASE_AMI_ID]    
  }
}