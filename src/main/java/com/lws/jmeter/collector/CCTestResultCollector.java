package com.lws.jmeter.collector;

import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.samplers.SampleEvent;

@Slf4j
public class CCTestResultCollector extends ResultCollector {

    public CCTestResultCollector() {

        super();

    }

    public CCTestResultCollector(Summariser summer) {

        super(summer);

    }

    @Override

    public void sampleOccurred(SampleEvent event) {

        super.sampleOccurred(event);

        log.info("sampleOccurred {}", event);
    }

}