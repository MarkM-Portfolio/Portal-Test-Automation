resource "aws_instance" "jenkins-agent" {
  ami                         = data.aws_ami.centos.id
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
    volume_size           = 20
  }

  subnet_id = "subnet-014047f30974086c8"

  vpc_security_group_ids = ["sg-0ddaf1862f39be2be"]

  connection {
    type = "ssh"
    host  = aws_instance.jenkins-agent.private_ip
    user  = "centos"
    agent = false
    private_key = file("./test-automation.pem")
  }

  provisioner "remote-exec" {
    inline = [
      "sudo yum -y update libgcc* libstdc++* glibc*",
      "sudo yum -y install libgcc*i686 libstdc++*i686 glibc*i686",
    ]
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

  provisioner "remote-exec" {
    inline = [
      "sudo sh ~/setupscripts/01-create-user-with-ssh.sh ${var.instance_name}${var.domain_suffix}",
      "sudo sh ~/setupscripts/02-setup-prereqs.sh ${var.instance_name}${var.domain_suffix}",
      "sudo sh ~/setupscripts/03-install-docker.sh",
      "sudo sh ~/setupscripts/04-prepare-jenkins-directories.sh",
      "sudo sh ~/setupscripts/05-move-to-nvme.sh",
      "sudo sh ~/setupscripts/06-setup-cron.sh",
    ]
  }
}

resource "aws_ec2_tag" "jenkins-agent-vol-name" {
  resource_id = element(tolist(aws_instance.jenkins-agent.root_block_device.*.volume_id), 0)
  key         = "Name"
  value       = var.instance_name
}

resource "aws_ec2_tag" "jenkins-agent-vol-owner" {
  resource_id = element(tolist(aws_instance.jenkins-agent.root_block_device.*.volume_id), 0)
  key         = "Owner"
  value       = var.instance_owner
}

resource "aws_ec2_tag" "jenkins-agent-vol-area" {
  resource_id = element(tolist(aws_instance.jenkins-agent.root_block_device.*.volume_id), 0)
  key         = "Area"
  value       = "BUILD"
}

resource "aws_ec2_tag" "jenkins-agent-vol-term-flag" {
  resource_id = element(tolist(aws_instance.jenkins-agent.root_block_device.*.volume_id), 0)
  key         = "termFlag"
  value       = "N"
}

# Create the Route53 record for the server
resource "aws_route53_record" "jenkins-agent" {
  zone_id = var.HOSTED_ZONE
  name    = var.instance_name
  type    = "A"
  ttl     = "300"
  records = ["${aws_instance.jenkins-agent.private_ip}"]
}
