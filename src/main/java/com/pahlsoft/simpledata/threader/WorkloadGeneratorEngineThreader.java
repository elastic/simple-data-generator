package com.pahlsoft.simpledata.threader;

import com.pahlsoft.simpledata.interfaces.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class WorkloadGeneratorEngineThreader {

    static Logger log = LoggerFactory.getLogger(WorkloadGeneratorEngineThreader.class);


    private WorkloadGeneratorEngineThreader() {
        throw new IllegalStateException("Utility class");
    }

    public static void runEngine(int threadCount, Engine engine)   {

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);

        try {

            for (int threads = 0; threads < threadCount; threads++) {
                    threadPoolExecutor.execute(engine);
            }

        } catch (Exception e) {
            if (log.isDebugEnabled()) { log.warn(e.getMessage()); }
        }

    }

}
