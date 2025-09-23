# Create the AWS AMI
resource "aws_instance" "dx-mavenized-components-ctf" {
  ami                         = data.aws_ami.cf17base.id
  instance_type               = var.aws_ec2_instance_type
  associate_public_ip_address = false
  key_name                    = "deployer-key"

  tags = {
    # The environment will be named after the test-run it has been created in
    Name      = var.TEST_RUN_ID
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

  connection {
    type = "ssh"
    user  = var.ec2_ssh_user
    agent = false
    private_key = file(var.private_key_file)
  }
}

# Create the Route53 record for the setup AMI
resource "aws_route53_record" "dx-mavenized-components-ctf" {
  zone_id = var.HOSTED_ZONE
  name    = var.TEST_RUN_ID
  type    = "A"
  ttl     = "300"
  records = ["${aws_instance.dx-mavenized-components-ctf.private_ip}"]
}
