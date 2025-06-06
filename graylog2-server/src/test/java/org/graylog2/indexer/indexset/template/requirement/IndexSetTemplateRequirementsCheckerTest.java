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
package org.graylog2.indexer.indexset.template.requirement;

import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.requirement.IndexSetTemplateRequirement.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexSetTemplateRequirementsCheckerTest {

    @Mock
    IndexSetTemplateRequirement requirement1;
    @Mock
    IndexSetTemplateRequirement requirement2;
    @Mock
    IndexSetTemplate indexSetTemplate;

    IndexSetTemplateRequirementsChecker underTest;
    private InOrder requirements;


    @BeforeEach
    void setUp() {
        when(requirement1.priority()).thenReturn(0);
        when(requirement2.priority()).thenReturn(1);
        requirements = Mockito.inOrder(requirement1, requirement2);
        LinkedHashSet<IndexSetTemplateRequirement> orderedSet = new LinkedHashSet<>();
        orderedSet.add(requirement2);
        orderedSet.add(requirement1);
        underTest = new IndexSetTemplateRequirementsChecker(orderedSet);
    }

    @Test
    void testPriorityOrder() {
        when(requirement1.check(any())).thenReturn(new Result(true, ""));
        when(requirement2.check(any())).thenReturn(new Result(true, ""));

        underTest.check(indexSetTemplate);

        requirements.verify(requirement1).check(any());
        requirements.verify(requirement2).check(any());
    }

    @Test
    void testRequirement1NotFulfilled() {
        Result expectedResult = new Result(false, "r1");
        when(requirement1.check(any())).thenReturn(expectedResult);

        Result result = underTest.check(indexSetTemplate);

        assertThat(result).isEqualTo(expectedResult);
        requirements.verify(requirement1).check(any());
        requirements.verify(requirement2, never()).check(any());
    }

    @Test
    void testRequirement2NotFulfilled() {
        Result expectedResult = new Result(false, "r2");
        when(requirement1.check(any())).thenReturn(new Result(true, ""));
        when(requirement2.check(any())).thenReturn(expectedResult);

        Result result = underTest.check(indexSetTemplate);

        assertThat(result).isEqualTo(expectedResult);
        requirements.verify(requirement1).check(any());
        requirements.verify(requirement2).check(any());
    }
}
