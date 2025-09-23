# Create the AWS EC2 instance
resource "aws_instance" "generic-latest-external" {
  ami                         = data.aws_ami.generic-ami-latest.id
  instance_type               = var.aws_ec2_instance_type
  associate_public_ip_address = true
  key_name                    = "test-automation-deployments"
  
  tags = {
    # The environment will be named after the test-run it has been created in
    Name      = var.TEST_RUN_ID
    Owner     = "howard.krovetz@hcl.com"
    Area      = "TEST"
    termFlag  = "N"
  }

  root_block_device {
    delete_on_termination = true
    volume_size           = 50
  }

  subnet_id = "subnet-021d6ef8ad5d03bc0"

  vpc_security_group_ids = ["sg-054baf4612c772571"]
}

# Create the Route53 record for the setup AMI
resource "aws_route53_record" "generic-latest-external" {
  zone_id = var.HOSTED_ZONE
  name    = var.ENV_HOSTNAME
  type    = "A"
  ttl     = "300"
  records = ["${aws_instance.generic-latest-external.public_ip}"]
}
