package org.slf4j.impl;

import org.junit.jupiter.api.Test;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginLoggerBridgeTest {

    @Test
    void formatsMessagesThroughJulFallback() {
        String loggerName = "bedwars-test-pool-logger";
        java.util.logging.Logger jul = java.util.logging.Logger.getLogger(loggerName);
        jul.setUseParentHandlers(false);
        jul.setLevel(java.util.logging.Level.ALL);
        CapturingHandler capture = new CapturingHandler();
        jul.addHandler(capture);
        try {
            StaticLoggerBinder.getSingleton().getLoggerFactory()
                    .getLogger(loggerName).info("Pool {} started", "test");
            assertEquals("Pool test started", capture.last.getMessage());
        } finally {
            jul.removeHandler(capture);
        }
    }

    private static final class CapturingHandler extends Handler {
        private LogRecord last;
        @Override public void publish(LogRecord record) { last = record; }
        @Override public void flush() { }
        @Override public void close() { }
    }
}
