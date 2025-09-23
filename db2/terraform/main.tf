resource "aws_instance" "db2-server" {
  ami                         = data.aws_ami.centos.id
  instance_type               = var.aws_ec2_instance_type
  associate_public_ip_address = var.use_public_ip
  key_name                    = "test-automation-deployments"

  tags = {
    Name          = var.instance_name
    Owner         = var.instance_owner
    Area          = "TEST"
    termFlag      = "N"
    POPOSchedule  = var.popo_schedule
  }

  root_block_device {
    delete_on_termination = true
    volume_size           = 20
  }

  subnet_id = var.aws_subnet

  vpc_security_group_ids = var.vpc_security_groups

  connection {
    type = "ssh"
    host  = var.use_public_ip ? aws_instance.db2-server.public_ip : aws_instance.db2-server.private_ip
    user  = "centos"
    agent = false
    private_key = file("./test-automation-deployments.pem")
  }
}

resource "aws_ec2_tag" "db2-server-vol-name" {
  resource_id = element(tolist(aws_instance.db2-server.root_block_device.*.volume_id), 0)
  key         = "Name"
  value       = var.instance_name
}

resource "aws_ec2_tag" "db2-server-vol-owner" {
  resource_id = element(tolist(aws_instance.db2-server.root_block_device.*.volume_id), 0)
  key         = "Owner"
  value       = var.instance_owner
}

resource "aws_ec2_tag" "db2-server-vol-area" {
  resource_id = element(tolist(aws_instance.db2-server.root_block_device.*.volume_id), 0)
  key         = "Area"
  value       = "BUILD"
}

resource "aws_ec2_tag" "db2-server-vol-term-flag" {
  resource_id = element(tolist(aws_instance.db2-server.root_block_device.*.volume_id), 0)
  key         = "termFlag"
  value       = "N"
}

# Create the Route53 record for the server
resource "aws_route53_record" "db2-server" {
  zone_id = "/hostedzone/${var.hosted_zone}"
  name    = "${var.instance_name}${var.domain_suffix}"
  type    = "A"
  ttl     = "300"
  records = ["${var.use_public_ip ? aws_instance.db2-server.public_ip : aws_instance.db2-server.private_ip}"]
}
