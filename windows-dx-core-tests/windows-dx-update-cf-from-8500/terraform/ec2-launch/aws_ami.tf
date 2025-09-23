data "aws_ami" "windows8500base" {
  owners      = ["657641368736"]
  most_recent = true

  filter {
    name    = "image-id"
    values = [var.BASE_AMI_ID]    
  }
}