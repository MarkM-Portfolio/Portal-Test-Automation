host=$1
mkdir /opt/IBM/WebSphere/HTTPServer/ssl

# Check for cert name in /root/.acme.sh directory
# If found get the cert name and overwrite the host variable
if [ -d /root/.acme.sh ]; then
   certname=$(find /root/.acme.sh -name ca.cer)
   if [ "$certname" != "" ]; then
      if [[ "$certname" != "${certname#/root/\.acme\.sh/}" ]]; then
         certname=${certname#/root/\.acme\.sh/}
         if [[ "$certname" != "${certname%/ca\.cer}" ]]; then
            host=${certname%/ca\.cer}
         fi
      fi
   fi
fi

sed -i '/LoadModule rewrite_module modules\/mod_rewrite.so/s/^#//g' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf

echo "host = $host"

# Create kdb file
/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -keydb -create -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -pw wpsadmin -type cms -expire 3600 -stash

# List certificates in the key file database
/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -cert -list -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -stashed -type cms

# Move apache certificate into a .p12 file (PKCS12 format) to import into the IBM HTTP Server .KDB file (CMS format) key database.
openssl pkcs12 -export -out /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.p12 -passout 'pass:wpsadmin' -inkey /root/.acme.sh/$host/$host.key -in /root/.acme.sh/$host/fullchain.cer -name fullchain

# Import the actual SSL cert or fullchain certificate
/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -cert -import -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.p12 -stashed -label fullchain -type pkcs12 -target /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -target_type cms -target_stashed

# Add intermediate certificate
/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -cert -add -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -stashed -file /root/.acme.sh/$host/ca.cer -label intermediateCA

# Add root certificate
/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -cert -add -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -stashed -file /root/.acme.sh/$host/trustid-x3-root.pem.der -label rootCA

# List certificates in the key file database
echo "List of the certificates in the key database"
/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -cert -list -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -stashed -type cms

# Validate each certificate in the certificate chain.
echo "Validating root CA certificate"
/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -cert -validate -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -stashed -label rootCA

echo "Validating intermediate CA certificate"
/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -cert -validate -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -stashed -label intermediateCA

echo "Validating fullchain or server certificate"
/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -cert -validate -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -stashed -label fullchain

# To check details for a particular certificate, including the CN of its signer
echo "Printing the full chain or server certificate"
/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -cert -details -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -label fullchain -stashed -type cms

# Set Lets Encrypt certificate as default certificate
/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -cert -setdefault -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -stashed -type cms -label fullchain

# Commented old logic for replace the config value for SSL (for refrence)
# sed -i '/LoadModule ibm\_ssl\_module modules\/mod\_ibm\_ssl\.so/s/^#//g' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf
# sed -i '/Listen 443/s/^#//g' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf
# sed -i '/SSLCheckCertificateExpiration 30/s/^#//g' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf
# sed -i '/\<VirtualHost \*\:443\>/s/^#//g' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf
# sed -i '/SSLEnable/s/^ #//g' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf
# sed -i '/^SSLEnable*/a SSLClientAuth None' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf
# sed -i '/Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"/s/^ #//g' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf
# sed -i '/\/VirtualHost\>/s/^#//g' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf
# sed -i '/^KeyFile /opt/IBM/HTTPServer/conf/ihsserverkey.kdb*/a KeyFile /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf 
# sed -i '/SSLDisable/s/^#//g' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf
# sed -i 's/\/opt\/HCL\/HTTPServer\/conf\/ihsserverkey.kdb/\/opt\/HCL\/HTTPServer\/ssl\/ihskeys.kdb/g' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf
# sed -i '/KeyFile \/opt\/HCL\/HTTPServer\/ssl\/ihskeys.kdb/a \ SSLStashFile \/opt\/HCL\/HTTPServer\/ssl\/ihskeys\.sth' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf

# Added new logic to add configuration for SSL
sed -i '/^# End of example SSL configuration.*/a \
\nLoadModule ibm_ssl_module modules\/mod_ibm_ssl.so \
Listen 443 \
SSLCheckCertificateExpiration 30 \
<VirtualHost *:443> \
SSLEnable \
SSLClientAuth None \
Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" \
</VirtualHost> \
KeyFile /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb \
SSLDisable \
\nLoadModule was_ap24_module /opt/IBM/WebSphere/ihsPlugins/bin/64bits/mod_was_ap24_http.so \
WebSpherePluginConfig /opt/IBM/WebSphere/ihsPlugins/config/webserver1/plugin-cfg.xml \
\nRewriteEngine on \
RewriteCond %{SERVER_PORT} =80 \
RewriteRule ^(.*) https://%{SERVER_NAME}%{REQUEST_URI} [R,L] \
RedirectMatch ^/$ /wps/portal/' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf

sed -i '/ssl-debug.log/s/^#//g' /opt/IBM/WebSphere/HTTPServer/conf/httpd.conf

chmod 775 /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.sth

/opt/IBM/WebSphere/HTTPServer/bin/gskcapicmd -keydb -stashpw -db /opt/IBM/WebSphere/HTTPServer/ssl/ihskeys.kdb -pw wpsadmin

/opt/IBM/WebSphere/AppServer/bin/wsadmin.sh -username $3 -password $4 -lang jython -f /tmp/dx-onpremise/scripts/helpers/http-server/update-plugin-cfg.py $2 $5

chmod 775 /opt/IBM/WebSphere/ihsPlugins/config/webserver1/plugin-key.sth
