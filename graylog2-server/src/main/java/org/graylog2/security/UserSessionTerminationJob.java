package org.graylog2.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerUpdate;

import javax.inject.Inject;

public class UserSessionTerminationJob implements Job {
    public static final String TYPE_NAME = "user-session-termination";

    private final UserSessionTerminationService sessionTerminationService;

    @Inject
    public UserSessionTerminationJob(UserSessionTerminationService sessionTerminationService) {
        this.sessionTerminationService = sessionTerminationService;
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        sessionTerminationService.runGlobalSessionTermination();
        return JobTriggerUpdate.withoutNextTime();
    }

    public interface Factory extends Job.Factory<UserSessionTerminationJob> {
        @Override
        UserSessionTerminationJob create(JobDefinitionDto jobDefinition);
    }

    @JsonTypeName(UserSessionTerminationJob.TYPE_NAME)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobDefinitionConfig implements org.graylog.scheduler.JobDefinitionConfig {
        @Override
        public String type() {
            return UserSessionTerminationJob.TYPE_NAME;
        }
    }
}
