# Create the Route53 record for the setup AMI
resource "aws_route53_record" "scheduled-kube-deploy" {
  zone_id = "/hostedzone/${var.hosted_zone}"
  name    = var.domain_name
  type    = var.record_type
  ttl     = "300"
  records = ["${var.ip_address}"]
  allow_overwrite = true
}