package com.lws.jmeter.job;

import com.lws.jmeter.service.ScriptService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@AllArgsConstructor
public class ScriptAutoStartJob {
    //todo we need to consider distributed case?

    private final ScriptService scriptService;

    @Scheduled(cron = "0/5 * * * * ?") // every 20 seconds
    public void startScript() {
        log.info("start running job");
        scriptService.runPendingScripts();
    }
}
