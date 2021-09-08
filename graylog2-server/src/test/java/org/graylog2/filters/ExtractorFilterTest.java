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
package org.graylog2.filters;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.graylog.failure.ProcessingFailureCause;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.inputs.Extractor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtractorFilterTest {

    private final InputService inputService;
    private final EventBus eventBus;
    private final ScheduledExecutorService executorService;
    private ExtractorFilter dut;

    public ExtractorFilterTest(@Mock InputService inputService, @Mock EventBus eventBus, @Mock ScheduledExecutorService executorService) {
        this.inputService = inputService;
        this.eventBus = eventBus;
        this.executorService = executorService;
    }

    @Test
    void testFailureHandling() throws NotFoundException {

        final Input input = mock(Input.class);
        when(input.getId()).thenReturn("123");
        when(inputService.all()).thenReturn(ImmutableList.of(input));

        final Extractor extractor = buildExceptionalExtractor();
        when(extractor.getTitle()).thenReturn("failing extractor");
        when(extractor.getId()).thenReturn("888");
        when(inputService.getExtractors(any())).thenReturn(ImmutableList.of(extractor));

        // extractors are initialized within constructor
        dut = new ExtractorFilter(inputService, eventBus, executorService);

        final Message message = new Message("message", "source", new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC));
        message.setSourceInputId("123");

        dut.filter(message);

        assertThat(message.processingErrors()).hasSize(1);
        assertThat(message.processingErrors().get(0)).satisfies(pe -> {
            assertThat(pe.getCause()).isEqualTo(ProcessingFailureCause.ExtractorException);
            assertThat(pe.getMessage()).isEqualTo("Could not apply extractor <failing extractor(888)>");
            assertThat(pe.getDetails()).isEqualTo("EIEIO!");
        });
    }

    private Extractor buildExceptionalExtractor() {
        final Extractor extractor = mock(Extractor.class);
        lenient().when(extractor.getOrder()).thenReturn(1L);
        lenient().doThrow(new RuntimeException("EIEIO!")).when(extractor).runExtractor(any());
        return extractor;
    }
}
