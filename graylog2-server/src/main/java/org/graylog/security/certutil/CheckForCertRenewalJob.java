package org.graylog.security.certutil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.JobTriggerUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class CheckForCertRenewalJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(CheckForCertRenewalJob.class);

    public static final String TYPE_NAME = "check-for-cert-renewal-execution-v1";

    private final CertRenewalService certRenewalService;

    public interface Factory extends Job.Factory<CheckForCertRenewalJob> {
        @Override
        CheckForCertRenewalJob create(JobDefinitionDto jobDefinition);
    }

    @AutoValue
    @JsonTypeName(TYPE_NAME)
    @JsonDeserialize(builder = CheckForCertRenewalJob.Config.Builder.class)
    public static abstract class Config implements JobDefinitionConfig {

        public static Config.Builder builder() {
            return Config.Builder.create();
        }

        public abstract Config.Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder implements JobDefinitionConfig.Builder<Config.Builder> {
            @JsonCreator
            public static Config.Builder create() {
                return new AutoValue_CheckForCertRenewalJob_Config.Builder().type(TYPE_NAME);
            }

            abstract Config autoBuild();

            public Config build() {
                // Make sure the type name is correct!
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }

    @Inject
    public CheckForCertRenewalJob(final CertRenewalService certRenewalService) {
        this.certRenewalService = certRenewalService;
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        LOG.debug("Job execute called {}", ctx);

        certRenewalService.checkAllDataNodes();

        return ctx.jobTriggerUpdates()
                .scheduleNextExecution()
                .toBuilder()
                .status(JobTriggerStatus.RUNNABLE)
                .build();
    }
}
