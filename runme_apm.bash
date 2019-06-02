java -javaagent:./apm_lib/elastic-apm-agent-1.6.1.jar \
-Delastic.apm.service_name=simple-data-generator \
-Delastic.apm.server_urls=<NEED_THIS> \
-Delastic.apm.secret_token=<NEED_THIS> \
-Delastic.apm.application_packages=com.pahlsoft.* \
-Djava.security.manager -Djava.security.policy=java.policy \
-jar ./build/libs/simple-data-generator-all-1.0-SNAPSHOT.jar examples/mortgage-data.yml
#-Delastic.apm.trace_methods=com.pahlsoft.simpledata.generator.WorkloadGenerator#buildDocument \
