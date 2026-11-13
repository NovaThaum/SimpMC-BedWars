package org.slf4j.impl;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;

/** Routes relocated SLF4J calls to this plugin's logger, with a JUL fallback. */
final class PluginLogger implements InvocationHandler {

    private final java.util.logging.Logger target;
    private final String name;
    private final Logger proxy;

    PluginLogger(String name) {
        this.name = name;
        this.target = resolvePluginLogger(name);
        this.proxy = (Logger) Proxy.newProxyInstance(Logger.class.getClassLoader(),
                new Class<?>[]{Logger.class}, this);
    }

    @Override
    public Object invoke(Object instance, Method method, Object[] args) {
        String methodName = method.getName();
        if ("getName".equals(methodName)) return name;
        if ("toString".equals(methodName)) return "PluginLogger(" + name + ")";
        if ("hashCode".equals(methodName)) return System.identityHashCode(instance);
        if ("equals".equals(methodName)) return instance == (args == null ? null : args[0]);
        if (methodName.startsWith("is") && methodName.endsWith("Enabled")) {
            return target.isLoggable(level(methodName.substring(2, methodName.length() - "Enabled".length())));
        }
        Level level = level(methodName);
        if (level == null) return null;

        int messageIndex = args != null && args.length > 0 && args[0] instanceof Marker ? 1 : 0;
        if (args == null || args.length <= messageIndex) return null;
        String format = String.valueOf(args[messageIndex]);
        Object[] values = Arrays.copyOfRange(args, messageIndex + 1, args.length);
        FormattingTuple tuple = MessageFormatter.arrayFormat(format, values);
        Throwable throwable = tuple.getThrowable();
        if (throwable == null) target.log(level, tuple.getMessage());
        else target.log(level, tuple.getMessage(), throwable);
        return null;
    }

    Logger asLogger() {
        return proxy;
    }

    private static Level level(String method) {
        switch (method.toLowerCase(Locale.ROOT)) {
            case "trace": return Level.FINEST;
            case "debug": return Level.FINE;
            case "info": return Level.INFO;
            case "warn": return Level.WARNING;
            case "error": return Level.SEVERE;
            default: return null;
        }
    }

    private static java.util.logging.Logger resolvePluginLogger(String name) {
        try {
            Class<?> pluginClass = Class.forName("com.andrei1058.bedwars.BedWars", false,
                    PluginLogger.class.getClassLoader());
            Object plugin = pluginClass.getField("plugin").get(null);
            if (plugin != null) {
                Object logger = plugin.getClass().getMethod("getLogger").invoke(plugin);
                if (logger instanceof java.util.logging.Logger) return (java.util.logging.Logger) logger;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // The plugin is not available yet; use a named JUL logger.
        }
        return java.util.logging.Logger.getLogger(name);
    }
}
