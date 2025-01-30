package org.graylog2.shared.buffers.processors;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

public record TimeStampConfig(@JsonProperty("grace_period") Duration gracePeriod) {
}
