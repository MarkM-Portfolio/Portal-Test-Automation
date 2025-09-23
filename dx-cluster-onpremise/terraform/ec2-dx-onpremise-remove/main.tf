# Create the AWS AMI
resource "aws_instance" "dx-onpremise" {
  ami                         = data.aws_ami.centos.id
  instance_type               = var.AWS_EC2_INSTANCE_TYPE
  associate_public_ip_address = false
  key_name                    = "test-automation-deployments"

  tags = {
    # The environment will be named after the test-run it has been created in
    Name     = var.instance_name
    Owner    = var.instance_owner
    Area     = "TEST"
    termFlag = "N"
  }

  root_block_device {
    delete_on_termination = true
    volume_size           = 100
  }

  #subnet_id = "subnet-033035ecf3a0e7ff4"

  #subnet_id = "subnet-021d6ef8ad5d03bc0" //Public01
  #subnet_id = "subnet-033035ecf3a0e7ff4" //Build01
  subnet_id = "subnet-014047f30974086c8" //Dev02

  vpc_security_group_ids = ["sg-0ddaf1862f39be2be"]

  connection {
    type = "ssh"
    host  = aws_instance.dx-onpremise.private_ip
    user  = "centos"
    agent = false
    private_key = file("./test-automation-deployments.pem")
  }
}

# Create the Route53 record for the setup AMI
resource "aws_route53_record" "dx-onpremise" {
  zone_id = var.HOSTED_ZONE
  name    = var.instance_name
  type    = "A"
  ttl     = "300"
  records = ["${aws_instance.dx-onpremise.private_ip}"]
}
