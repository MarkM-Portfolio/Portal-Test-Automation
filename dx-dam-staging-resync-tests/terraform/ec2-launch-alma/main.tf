# Create the AWS AMI
resource "aws_instance" "generic-latest" {
  ami                         = data.aws_ami.alma.id
  instance_type               = var.aws_ec2_instance_type
  associate_public_ip_address = false
  key_name                    = "test-automation-deployments"

  tags = {
    # The environment will be named after the test-run it has been created in
    Name      = var.instance_name
    Owner     = var.instance_owner
    Area      = "TEST"
    termFlag  = "N"
    Savings   = "POPO_Manual"
    expires   = var.EXPIRATION_STAMP
  }

  root_block_device {
    delete_on_termination = true
    volume_size           = 50
  }

  subnet_id = var.EC2_SUBNET

  vpc_security_group_ids = ["sg-0ddaf1862f39be2be"]

  connection {
    type = "ssh"
    host  = aws_instance.dx-default-test.private_ip
    user  = var.ec2_ssh_user
    agent = false
    private_key = file(var.private_key_file)
  }
}

# Create the Route53 record for the setup AMI
resource "aws_route53_record" "generic-latest" {
  zone_id = var.HOSTED_ZONE
  name    = "TAG_NAME"
  type    = "A"
  ttl     = "300"
  records = ["${aws_instance.generic-latest.private_ip}"]
  allow_overwrite = true
}
