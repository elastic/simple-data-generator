package com.pahlsoft.simpledata.runner;

import com.pahlsoft.simpledata.clients.ElasticsearchClientUtil;
import com.pahlsoft.simpledata.engine.ElasticsearchWorkloadGeneratorEngine;
import com.pahlsoft.simpledata.model.Configuration;
import com.pahlsoft.simpledata.model.Workload;
import com.pahlsoft.simpledata.threader.WorkloadGeneratorEngineThreader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;

public class EngineRunner {
    static Logger log = LoggerFactory.getLogger(EngineRunner.class);

    public static void main(String[] args) {
        validateArguments(args);
        Configuration configuration = loadConfig(args[0]);

        try {
            // Create Engine for each workload
            Iterator iterator = configuration.getWorkloads().iterator();

            while (iterator.hasNext()) {
                Workload workload;
                workload = (Workload) iterator.next();
                        // TODO: May need to create something cleaner for ALL backend types but for now this works.
                        ElasticsearchClientUtil.setupElasticsearch(configuration,workload);
                        WorkloadGeneratorEngineThreader.runEngine(workload.getWorkloadThreads(), new ElasticsearchWorkloadGeneratorEngine(configuration,workload));

                }
            if (log.isDebugEnabled()) {
                debugConfiguration(configuration);
            }
            log.info("Workloads Started");
            System.out.printf("%d Workloads Started.", configuration.getWorkloads().size());

        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("Initialization Error: access to yml is restricted or incorrectly configured");
        }

    }

    private static void debugConfiguration(Configuration configuration) {
        log.debug("Endpoint: " + configuration.getElasticsearchHost() );
        log.debug("Endpoint HTTP Scheme: " + configuration.getElasticsearchScheme() );
        log.debug("Port: " + configuration.getElasticsearchPort() );
        log.debug("ApiKeyID: " + configuration.getElasticsearchApiKeyId());
        log.debug("ApiKeySecret: " + configuration.getElasticsearchApiKeySecret() );
        log.debug("User: " + configuration.getElasticsearchUser() );
        log.debug("Password: " + configuration.getElasticsearchPassword() );
        log.debug("Number of Workloads " + configuration.getWorkloads().size());
    }

    private static Configuration loadConfig(String args) {
        Configuration configuration = null;
        try {
            try
            {
                Yaml yaml = new Yaml(new Constructor(Configuration.class));
                InputStream inputStream = new FileInputStream(new File(args));
               configuration = yaml.load(inputStream);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } catch ( Exception e) {
            System.out.println("Initialization Error: Unable to Load YML model file");
            log.error("Initialization Error: Unable to Load YML model file");
            System.exit(1);
        }
        log.info("Configuration Loaded");
        System.out.println("Configuration Loaded.");
        return configuration;
    }

    private static void validateArguments(String[] args) {
        if (args.length !=1 ) {
            System.out.println("Error - Improper Usage, try: sample-data-generator-all*.jar your_config_file.yml ");
            log.error("Error - Improper Usage, try: sample-data-generator-all*.jar your_config_file.yml ");
            System.exit(1);
        }
    }

}
