# clean postgresql
cd /opt/media-library
docker-compose down -v

# replace artifactory in the yaml
sed -i "s/quintana-docker.artifactory.cwp.pnp-hcl.com/$ARTIFACTORY_HOST/g" docker-compose.yaml

# find the latest matching version for postgresql
curl $ARTIFACTORY_IMAGE_BASE_URL/portal/persistence/postgres/ -o pg.list
cat pg.list;
sed -e 's/<[^>]*>//g' pg.list > pg-clean.list
grep '[0-9]-[0-9]' pg-clean.list | grep "$MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER"  > pg-rel.list
cat pg-rel.list | tr -s '[:blank:]' ',' > pg-rel.csv
# transform all dates to a sortable format
while read line; do
        var1=$(echo "$line" | cut -d ',' -f 1)
        var2=$(echo "$line" | cut -d ',' -f 2)
        var2=$(date --date="$(printf "01 %s" $var2)" +"%Y-%m-%d")
        echo "$var1,$var2" >> pg-transformed.csv
done < pg-rel.csv
# get the latest image tag (first line of csv)
PERSISTENCE_VERSION=$(sort -t, -k2,2 -nr pg-transformed.csv | head -n 1 | cut -d ',' -f 1)
PERSISTENCE_VERSION=${PERSISTENCE_VERSION%/}

echo "Using image tag $PERSISTENCE_VERSION for the postgresql"

# replace the version in the 
sed -i "s/##version-placeholder##/$PERSISTENCE_VERSION/g" docker-compose.yaml

# start postgresql
cd /opt/media-library
sleep 10
docker-compose up -d
echo "Waiting 10s to give DB time for startup."
sleep 10