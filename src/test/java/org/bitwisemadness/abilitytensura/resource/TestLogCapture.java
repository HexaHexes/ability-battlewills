package org.bitwisemadness.abilitytensura.resource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;

/**
 * Minimal in-test log capture (mirror of core's fixture, which is not
 * published with the artifact): attaches an in-memory appender to the named
 * logger at the requested level and restores the previous state on close -
 * always use try-with-resources.
 */
final class TestLogCapture implements AutoCloseable {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final LoggerContext context;
    private final String loggerName;
    private final LoggerConfig loggerConfig;
    private final boolean preexistingLoggerConfig;
    private final Level previousLevel;
    private final CapturingAppender appender;

    private TestLogCapture(LoggerContext context, String loggerName, LoggerConfig loggerConfig,
            boolean preexistingLoggerConfig, Level previousLevel, CapturingAppender appender) {
        this.context = context;
        this.loggerName = loggerName;
        this.loggerConfig = loggerConfig;
        this.preexistingLoggerConfig = preexistingLoggerConfig;
        this.previousLevel = previousLevel;
        this.appender = appender;
    }

    static TestLogCapture open(Class<?> loggerOwner, Level level) {
        String loggerName = loggerOwner.getName();
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        LoggerConfig before = config.getLoggerConfig(loggerName);
        boolean preexisting = loggerName.equals(before.getName());
        Level previousLevel = before.getLevel();

        Configurator.setLevel(loggerName, level);
        LoggerConfig target = config.getLoggerConfig(loggerName);

        CapturingAppender appender =
                new CapturingAppender("test-log-capture-" + SEQUENCE.incrementAndGet());
        appender.start();
        target.addAppender(appender, Level.ALL, null);

        return new TestLogCapture(context, loggerName, target, preexisting, previousLevel, appender);
    }

    List<String> messages() {
        return List.copyOf(appender.messages);
    }

    @Override
    public void close() {
        loggerConfig.removeAppender(appender.getName());
        appender.stop();
        if (preexistingLoggerConfig) {
            if (previousLevel != null) {
                Configurator.setLevel(loggerName, previousLevel);
            }
        } else {
            context.getConfiguration().removeLogger(loggerName);
        }
    }

    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> messages = new CopyOnWriteArrayList<>();

        private CapturingAppender(String name) {
            super(name, null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }
    }
}
