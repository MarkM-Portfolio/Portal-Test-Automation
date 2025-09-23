# Create the AWS AMI
resource "aws_instance" "dx-cf-wtf-1-ps" {
  ami                         = data.aws_ami.dx-update-cf-v95.id
  instance_type               = var.aws_ec2_instance_type
  associate_public_ip_address = false
  key_name                    = "deployer-key"

  tags = {
    # The environment will be named after the test-run it has been created in
    Name            = var.TEST_RUN_ID
    Owner           = "howard.krovetz@hcl.com"
    Area            = "TEST"
    termFlag        = "N"
    # This timestamp will be used by housekeeping to determine expired resources
    expires         = var.EXPIRATION_STAMP
    # This build label can be used by other following tasks to determine which build to pickup
    build           = data.aws_ami.dx-update-cf-v95.tags.build
    baseImageName   = data.aws_ami.dx-update-cf-v95.tags.Name
  }

  root_block_device {
    delete_on_termination = true
  }

  subnet_id = "subnet-033035ecf3a0e7ff4"

  vpc_security_group_ids = ["sg-0ddaf1862f39be2be"]
}
