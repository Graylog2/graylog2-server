package org.graylog.scheduler.rest;

import com.google.common.collect.ImmutableList;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.security.UserContext;

import java.util.List;
import java.util.Optional;

/**
 * Pluggable interface for common job scheduler resource tasks.
 * Implementations are responsible on checking the permissions of the {@link UserContext}
 */
public interface JobResourceHandler {

    default List<JobTriggerDto> listAllJobs(UserContext userContext) {
        return ImmutableList.of();
    }

    default Optional<JobTriggerDto> getJob(UserContext userContext, String jobID) {
        return Optional.empty();
    }

    default Optional<JobTriggerDto> cancelJob(UserContext userContext, String jobId) {
        return Optional.empty();
    }

    default JobTriggerDetails getTriggerDetails(JobTriggerDto triggerDto) {
        return JobTriggerDetails.EMPTY_DETAILS;
    }
}
