package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PluginLoggerFactory implements ILoggerFactory {

    private final Map<String, PluginLogger> loggers = new ConcurrentHashMap<>();

    @Override
    public Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, PluginLogger::new).asLogger();
    }
}
