# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

export http_proxy=$1
export https_proxy=$1
printenv | sort

# clear docker space for safety
docker stop $(docker ps -aq) || true
docker rm $(docker ps -aq) || true
docker rmi $(docker images -q) || true
docker network rm internal || true

# Clear directories for safety
sudo rm -rf /home/centos/grafana
sudo rm -f /home/centos/prometheus.yml

# Prepare firewall config
sudo firewall-cmd --zone=public --permanent --add-port=9090/tcp
sudo firewall-cmd --zone=public --permanent --add-port=9100/tcp
sudo firewall-cmd --zone=public --permanent --add-port=80/tcp
sudo firewall-cmd --reload

# Create an internal docker network for inter-service communication
docker network create internal

# Configure prometheus to look up the node exporter by the host IP
HOST_IP=$(hostname -I | awk '{print $1}')
cp prometheus.template.yml prometheus.yml
sed -i "s|TARGET_HOST|$HOST_IP|g" prometheus.yml

# Create prometheus data directory
mkdir -p /home/centos/prometheus
chmod -R 777 /home/centos/prometheus

# Run prometheus server
docker run --name prometheus -d -p 9090:9090 --net=internal --restart always \
        -v /home/centos/prometheus:/prometheus \
        -v /home/centos/prometheus.yml:/etc/prometheus/prometheus.yml \
        quintana-docker.artifactory.cwp.pnp-hcl.com/utility/prom/prometheus:v2.30.3

# Run node exporter natively, since it needs access to NFS
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/utility/node_exporter-1.3.1.linux-amd64.tar.gz -o node_exporter.tar.gz
tar zvxf node_exporter.tar.gz
mv /home/centos/node_exporter-1.3.1.linux-amd64 /home/centos/node_exporter
chmod +x /home/centos/node_exporter/node_exporter

# Create systemd service for the node exporter
sudo bash -c 'cat <<EOF > /etc/systemd/system/node_exporter.service
[Unit]
Description=Node Exporter
After=network.target
StartLimitIntervalSec=0
[Service]
Type=simple
Restart=always
RestartSec=1
User=centos
ExecStart=/home/centos/node_exporter/node_exporter

[Install]
WantedBy=multi-user.target
EOF'

# Enable node exporter service
sudo systemctl enable --now node_exporter

# Create grafana static dir
mkdir -p /home/centos/grafana
chmod -R 777 /home/centos/grafana

# Run Grafana
docker run --name grafana -d -p 3000:3000 --net internal --restart always \
        -e GF_SERVER_ROOT_URL=http://localhost:3000/grafana \
	-e GF_SECURITY_ADMIN_PASSWORD=p0rtal4u \
        -v /home/centos/grafana:/var/lib/grafana \
	-v /home/centos/provisioning:/etc/grafana/provisioning \
        quintana-docker.artifactory.cwp.pnp-hcl.com/utility/grafana/grafana:8.2.2

# Run NGINX as gateway
docker run --name nginx -d -p 80:80 --net internal --restart always \
        -v /home/centos/nginx:/usr/share/nginx/html:ro \
        -v /home/centos/nginx.conf:/etc/nginx/nginx.conf:ro \
        quintana-docker.artifactory.cwp.pnp-hcl.com/utility/nginx:1.21.3

# Run System Report script via crontab, every five minutes
(crontab -l 2>/dev/null; echo "*/5 * * * * /bin/bash /home/centos/generate-report.sh") | crontab -
