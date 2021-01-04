/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.integrations;

import org.graylog.integrations.audit.IntegrationsAuditEventTypes;
import org.graylog.integrations.aws.AWSPermissions;
import org.graylog.integrations.aws.codecs.AWSCodec;
import org.graylog.integrations.aws.codecs.KinesisCloudWatchFlowLogCodec;
import org.graylog.integrations.aws.codecs.KinesisRawLogCodec;
import org.graylog.integrations.aws.inputs.AWSInput;
import org.graylog.integrations.aws.resources.AWSResource;
import org.graylog.integrations.aws.resources.KinesisSetupResource;
import org.graylog.integrations.aws.transports.AWSTransport;
import org.graylog.integrations.aws.transports.KinesisTransport;
import org.graylog.integrations.inputs.paloalto.PaloAltoCodec;
import org.graylog.integrations.inputs.paloalto.PaloAltoTCPInput;
import org.graylog.integrations.inputs.paloalto9.PaloAlto9xCodec;
import org.graylog.integrations.inputs.paloalto9.PaloAlto9xInput;
import org.graylog.integrations.ipfix.codecs.IpfixCodec;
import org.graylog.integrations.ipfix.inputs.IpfixUdpInput;
import org.graylog.integrations.ipfix.transports.IpfixUdpTransport;
import org.graylog.integrations.notifications.types.SlackEventNotification;
import org.graylog.integrations.notifications.types.SlackEventNotificationConfig;
import org.graylog.integrations.pagerduty.PagerDutyNotification;
import org.graylog.integrations.pagerduty.PagerDutyNotificationConfig;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder;

import java.util.Collections;
import java.util.Set;

/**
 * Extend the PluginModule abstract class here to add you plugin to the system.
 */
public class IntegrationsModule extends PluginModule {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationsModule.class);

    /**
     * Returns all configuration beans required by this plugin.
     * <p>
     * Implementing this method is optional. The default method returns an empty {@link Set}.
     */
    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {
        configureServerOnlyBindings();
        configureUniversalBindings();
    }

    private void configureServerOnlyBindings() {
        if (!isForwarder()) {
            /*
             * Register your plugin types here.
             *
             * Examples:
             *
             * addMessageInput(Class<? extends MessageInput>);
             * addMessageFilter(Class<? extends MessageFilter>);
             * addMessageOutput(Class<? extends MessageOutput>);
             * addPeriodical(Class<? extends Periodical>);
             * addAlarmCallback(Class<? extends AlarmCallback>);
             * addInitializer(Class<? extends Service>);
             * addRestResource(Class<? extends PluginRestResource>);
             *
             * Add all configuration beans returned by getConfigBeans():
             *
             * addConfigBeans();
             */

            addAuditEventTypes(IntegrationsAuditEventTypes.class);

            // Slack Notification
            addNotificationType(SlackEventNotificationConfig.TYPE_NAME,
                    SlackEventNotificationConfig.class,
                    SlackEventNotification.class,
                    SlackEventNotification.Factory.class);

            // Pager Duty Notification
            addNotificationType(
                    PagerDutyNotificationConfig.TYPE_NAME,
                    PagerDutyNotificationConfig.class,
                    PagerDutyNotification.class,
                    PagerDutyNotification.Factory.class);
        }
    }

    /**
     * Place bindings here that need to run in the Graylog Server and the Forwarder.
     * Please do not add any bindings here that use MongoDB since the Forwarder does not have access to MongoDB.
     * In general, this should only contain input/codec/transport bindings that are supported in the Forwarder
     * and do not use MongoDB.
     */
    private void configureUniversalBindings() {
        // IPFIX
        addMessageInput(IpfixUdpInput.class);
        addCodec("ipfix", IpfixCodec.class);
        addTransport("ipfix-udp", IpfixUdpTransport.class);

        // Palo Alto Networks 8x
        LOG.debug("Registering message input: {}", PaloAltoTCPInput.NAME);
        addMessageInput(PaloAltoTCPInput.class);
        addCodec(PaloAltoCodec.NAME, PaloAltoCodec.class);

        // Palo Alto Networks 9x
        LOG.debug("Registering message input: {}", PaloAlto9xInput.NAME);
        addMessageInput(PaloAlto9xInput.class);
        addCodec(PaloAlto9xCodec.NAME, PaloAlto9xCodec.class);

        // AWS
        addCodec(AWSCodec.NAME, AWSCodec.class);
        addCodec(KinesisCloudWatchFlowLogCodec.NAME, KinesisCloudWatchFlowLogCodec.class);
        addCodec(KinesisRawLogCodec.NAME, KinesisRawLogCodec.class);
        addMessageInput(AWSInput.class);
        addPermissions(AWSPermissions.class);
        addRestResource(AWSResource.class);
        addRestResource(KinesisSetupResource.class);
        addTransport(AWSTransport.NAME, AWSTransport.class);
        addTransport(KinesisTransport.NAME, KinesisTransport.class);
        bind(IamClientBuilder.class).toProvider(IamClient::builder);
        bind(CloudWatchLogsClientBuilder.class).toProvider(CloudWatchLogsClient::builder);
        bind(KinesisClientBuilder.class).toProvider(KinesisClient::builder);
    }

    /**
     * @return A boolean indicating if the plugin is being loaded within the Graylog Forwarder.
     * The graylog.forwarder system property is set in the startup sequence of the Graylog Cloud Forwarder.
     * <p>
     * The Cloud Forwarder only supports inputs. This allows other bindings to be skipped when this plugin is
     * loaded within the Cloud Forwarder.
     */
    boolean isForwarder() {
        return Boolean.parseBoolean(System.getProperty("graylog.forwarder"));
    }
}
