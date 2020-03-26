java -javaagent:../apm_lib/elastic-apm-agent-1.14.0.jar \
-Delastic.apm.service_name=simple-data-generator \
-Delastic.apm.server_urls=<NEED_THIS> \
-Delastic.apm.secret_token=<NEED_THIS> \
-Delastic.apm.application_packages=com.pahlsoft.* \
-Delastic.apm.log_level=WARN \
-Djava.security.manager -Djava.security.policy=../java.policy \
-jar ../build/libs/simple-data-generator-1.0.0-SNAPSHOT.jar ../examples/max-disk-generator-data.yml
