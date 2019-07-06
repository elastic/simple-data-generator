java -javaagent:./apm_lib/elastic-apm-agent-1.6.1.jar \
-Delastic.apm.service_name=simple-data-generator \
-Delastic.apm.server_urls=http://3d4d320293b84522a9e19068a6be6d7a.192.168.0.156.ip.es.io:9200 \
-Delastic.apm.secret_token=ECd7ZasCwQlHjjkvgS \
-Delastic.apm.application_packages=com.pahlsoft.* \
-Delastic.apm.log_level=WARN \
-Djava.security.manager -Djava.security.policy=java.policy \
-jar ./build/libs/simple-data-generator-all-1.0-SNAPSHOT.jar examples/max-disk-generator-data.yml
