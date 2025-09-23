resource "aws_instance" "jenkins-master" {
  ami                         = data.aws_ami.centos.id
  instance_type               = var.aws_ec2_instance_type
  associate_public_ip_address = false
  key_name                    = "test-automation-deployments"

  tags = {
    Name          = var.instance_name
    Owner         = var.instance_owner
    Area          = "INFRA"
    termFlag      = "N"
    POPOSchedule  = "n/a"
    Savings       = "POPO_Manual"
  }

  root_block_device {
    delete_on_termination = true
    volume_size           = var.aws_ec2_disk_size
  }

  subnet_id = "subnet-014047f30974086c8"

  vpc_security_group_ids = [
    "sg-0ddaf1862f39be2be",
    "sg-0f9ead74405151fbd",
    "sg-05cb29a52f39b7d2a",
  ]
  
  connection {
    type = "ssh"
    host  = aws_instance.jenkins-master.private_ip
    user  = "centos"
    agent = false
    private_key = file("./test-automation.pem")
  }

  provisioner "remote-exec" {
    inline = [
      "mkdir ~/setupscripts"
    ]
  }

  provisioner "file" {
    source      = "../setupscripts/"
    destination = "~/setupscripts"
  }
}

resource "aws_ec2_tag" "jenkins-master-vol-name" {
  resource_id = element(tolist(aws_instance.jenkins-master.root_block_device.*.volume_id), 0)
  key         = "Name"
  value       = var.instance_name
}

resource "aws_ec2_tag" "jenkins-master-vol-owner" {
  resource_id = element(tolist(aws_instance.jenkins-master.root_block_device.*.volume_id), 0)
  key         = "Owner"
  value       = var.instance_owner
}

resource "aws_ec2_tag" "jenkins-master-vol-area" {
  resource_id = element(tolist(aws_instance.jenkins-master.root_block_device.*.volume_id), 0)
  key         = "Area"
  value       = "INFRA"
}

resource "aws_ec2_tag" "jenkins-master-vol-term-flag" {
  resource_id = element(tolist(aws_instance.jenkins-master.root_block_device.*.volume_id), 0)
  key         = "termFlag"
  value       = "N"
}

# Create the Route53 record for the server
resource "aws_route53_record" "jenkins-master" {
  zone_id = var.HOSTED_ZONE
  name    = var.instance_name
  type    = "A"
  ttl     = "300"
  records = ["${aws_instance.jenkins-master.private_ip}"]
}
