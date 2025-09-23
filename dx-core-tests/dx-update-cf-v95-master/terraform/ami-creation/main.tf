# Create the AWS AMI
resource "aws_ami_from_instance" "dx-update-cf-v95-master" {
  name               = var.TEST_RUN_ID
  source_instance_id = var.TEST_EC2_ID
  tags = {
    Name      = var.TEST_RUN_ID
    Owner     = "brian.anderson@hcl.com"
    Area      = "TEST"
    termFlag  = "N"
    # This timestamp will be used by housekeeping to determine expired resources
    expires   = var.EXPIRATION_STAMP
    # This build label can be used by other following tasks to determine which build to pickup
    build     = var.BUILD_LABEL
  }
}

