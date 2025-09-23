data "aws_ami" "cf17base" {
  owners      = ["657641368736"]
  most_recent = true

  filter {
    name    = "image-id"
    values = [var.BASE_AMI_ID]    
  }
}