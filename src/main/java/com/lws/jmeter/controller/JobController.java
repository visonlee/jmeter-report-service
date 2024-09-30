package com.lws.jmeter.controller;

import com.lws.jmeter.service.ScriptService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class JobController {

    private final ScriptService scriptService;

    @GetMapping("/runJob")
    @Deprecated
    public String runJob() {
        log.info("start running job");
        scriptService.runPendingScripts();
        return "ok";
    }
}
