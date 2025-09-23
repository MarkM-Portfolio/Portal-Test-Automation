resource "aws_instance" "jenkins-agent" {
  ami                         = data.aws_ami.alma.id
  instance_type               = var.aws_ec2_instance_type
  associate_public_ip_address = false
  key_name                    = "test-automation-deployments"

  tags = {
    Name     = var.instance_name
    Owner    = var.instance_owner
    Area     = "BUILD"
    termFlag = "N"
  }

  root_block_device {
    delete_on_termination = true
    volume_size           = 100
  }

  subnet_id = "subnet-014047f30974086c8"

  vpc_security_group_ids = ["sg-0ddaf1862f39be2be"]

  connection {
    type = "ssh"
    host  = aws_instance.jenkins-agent.private_ip
    user  = var.ec2_ssh_user
    agent = false
    private_key = file(var.private_key_file)
  }
}

# Create the Route53 record for the server
resource "aws_route53_record" "jenkins-agent" {
  zone_id = var.HOSTED_ZONE
  name    = var.instance_name
  type    = "A"
  ttl     = "300"
  records = ["${aws_instance.jenkins-agent.private_ip}"]
}
