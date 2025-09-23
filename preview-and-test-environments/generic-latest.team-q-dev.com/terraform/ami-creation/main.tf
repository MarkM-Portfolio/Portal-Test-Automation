# Create the AWS AMI
resource "aws_ami_from_instance" "generic-ami-latest" {
  name               = var.TEST_RUN_ID
  source_instance_id = var.TEST_EC2_ID
  tags = {
    Name      = var.TEST_RUN_ID
    Owner     = "howard.krovetz@hcl.com"
    Area      = "TEST"
    termFlag  = "N"
  }
  timeouts {
    create    = "90m"
  }
}

