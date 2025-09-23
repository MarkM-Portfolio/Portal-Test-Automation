data "aws_ami" "generic-ami-latest" {
  owners      = ["657641368736"]
  most_recent = true
  
  filter {
    name   = "name"
    values = [var.TEST_RUN_ID]
  }

}
