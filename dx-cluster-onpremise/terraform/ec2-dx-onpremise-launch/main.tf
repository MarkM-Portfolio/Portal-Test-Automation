# Create the AWS AMI
resource "aws_instance" "dx-onpremise" {
  ami                         = data.aws_ami.dx-update-cf-v95.id
  instance_type               = var.AWS_EC2_INSTANCE_TYPE
  associate_public_ip_address = var.use_public_ip
  key_name                    = "test-automation-deployments"

  tags = {
    # The environment will be named after the test-run it has been created in
    Name      = var.instance_name
    Owner     = var.instance_owner
    Area      = "TEST"
    termFlag  = "N"
    POPOSchedule = var.popo_schedule
    # This build label can be used by other following tasks to determine which build to pickup
    build           = data.aws_ami.dx-update-cf-v95.tags.build
    baseImageName   = data.aws_ami.dx-update-cf-v95.tags.Name
  }

  root_block_device {
    delete_on_termination = true
    volume_size           = 200
  }

  #subnet_id = "subnet-033035ecf3a0e7ff4"

  #subnet_id = "subnet-021d6ef8ad5d03bc0" //Public01
  #subnet_id = "subnet-033035ecf3a0e7ff4" //Build01
  subnet_id = var.aws_subnet

  vpc_security_group_ids = var.vpc_security_groups

  connection {
    type = "ssh"
    host  = var.use_public_ip ? aws_instance.dx-onpremise.public_ip : aws_instance.dx-onpremise.private_ip
    user  = var.instance_user
    agent = false
    private_key = file("./test-automation-deployments.pem")
  }

  provisioner "remote-exec" {
    inline = [
      "mkdir /tmp/dx-onpremise",
    ]
  }

  provisioner "file" {
    source      = "../../scripts"
    destination = "/tmp/dx-onpremise"
  }
}

# Create the Route53 record for the setup AMI
resource "aws_route53_record" "dx-onpremise" {
  zone_id = "/hostedzone/${var.HOSTED_ZONE}"
  name    = "${var.instance_name}${var.domain_suffix}"
  type    = "A"
  ttl     = "300"
  records = ["${var.use_public_ip ? aws_instance.dx-onpremise.public_ip : aws_instance.dx-onpremise.private_ip}"]
}