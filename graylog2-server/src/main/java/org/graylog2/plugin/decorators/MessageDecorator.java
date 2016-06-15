package org.graylog2.plugin.decorators;

import org.graylog2.decorators.Decorator;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.AbstractDescriptor;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.ConfigurationRequest;

import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface MessageDecorator extends Function<List<ResultMessage>, List<ResultMessage>> {
    interface Factory {
        MessageDecorator create(Decorator decorator);
        Config getConfig();
        Descriptor getDescriptor();
    }

    interface Config {
        ConfigurationRequest getRequestedConfiguration();
    }

    abstract class Descriptor extends AbstractDescriptor {
        private final String humanName;

        protected Descriptor() {
            throw new IllegalStateException("This class should not be instantiated directly, this is a bug.");
        }

        public Descriptor(String name, boolean exclusive, String linkToDocs, String humanName) {
            super(name, exclusive, linkToDocs);
            this.humanName = humanName;
        }

        public String getHumanName() {
            return humanName;
        }
    }
}
