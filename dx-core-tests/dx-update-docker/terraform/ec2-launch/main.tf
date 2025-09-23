# Create the AWS AMI
resource "aws_instance" "dx-update-docker" {
  ami                         = data.aws_ami.cf16base.id
  instance_type               = var.aws_ec2_instance_type
  associate_public_ip_address = false
  key_name                    = "test-automation-deployments"

  tags = {
    # The environment will be named after the test-run it has been created in
    Name      = "TAG_NAME"
    Owner     = "howard.krovetz@hcl.com"
    Area      = "TEST"
    termFlag  = "N"
    # This timestamp will be used by housekeeping to determine expired resources
    expires   = var.EXPIRATION_STAMP
    # This build label can be used by other following tasks to determine which build to pickup
    build     = var.BUILD_LABEL
  }

  root_block_device {
    delete_on_termination = true
  }

  subnet_id = "subnet-033035ecf3a0e7ff4"

  vpc_security_group_ids = ["sg-0ddaf1862f39be2be"]
}

# Create the Route53 record for the setup AMI
resource "aws_route53_record" "dx-update-docker" {
  zone_id = var.HOSTED_ZONE
  name    = "TAG_NAME"
  type    = "A"
  ttl     = "300"
  records = ["${aws_instance.dx-update-docker.private_ip}"]
}