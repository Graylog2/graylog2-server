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
package org.graylog.plugins.pipelineprocessor.db.memory;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

public class InMemoryRuleServiceTest {
    private InMemoryRuleService service;

    @Before
    public void setup() {
        service = new InMemoryRuleService(new ClusterEventBus());
    }

    @Test
    public void notFoundException() {

        try {
            service.load("1");
            fail("Should throw an exception");
        } catch (NotFoundException e) {
            assertThat(e).hasMessage("No such rule with id 1");
        }
    }

    @Test
    public void storeRetrieve() {
        RuleDao rule = RuleDao.create(null, "test", "description", "rule \"test\" when true then end", null, null);
        final RuleDao savedRule = service.save(rule);

        // saving should create a copy with an id
        assertThat(savedRule).isNotEqualTo(rule);
        assertThat(savedRule.id()).isNotNull();

        RuleDao loaded;
        try {
            loaded = service.load(savedRule.id());
        } catch (NotFoundException e) {
            fail("The rule should be found");
            loaded = null;
        }
        assertThat(loaded).isNotNull();
        assertThat(loaded).isEqualTo(savedRule);

        service.delete(loaded.id());
        try {
            service.load(loaded.id());
            fail("Deleted rules should not be found anymore");
        } catch (NotFoundException ignored) {
        }
    }

    @Test
    public void loadByName() throws NotFoundException {
        RuleDao rule = RuleDao.create(null, "test", "description", "rule \"test\" when true then end", null, null);
        final RuleDao savedRule = service.save(rule);
        final RuleDao loadedRule = service.loadByName(savedRule.title());
        assertThat(loadedRule).isEqualTo(savedRule);
    }

    @Test
    public void loadByNameNotFound() {
        assertThatThrownBy(() -> service.loadByName("Foobar"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No rule with name Foobar");
    }

    @Test
    public void uniqueTitles() {
        RuleDao rule = RuleDao.create(null, "test", "description", "rule \"test\" when true then end", null, null);
        RuleDao rule2 = RuleDao.create(null,
                                       "test",
                                       "some other description",
                                       "rule \"test\" when false then end",
                                       null,
                                       null);

        final RuleDao saved = service.save(rule);
        try {
            service.save(rule2);
            fail("Titles must be unique for two different rules");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            service.save(saved.toBuilder().createdAt(DateTime.now(DateTimeZone.UTC)).build());
        } catch (IllegalArgumentException e) {
            fail("Updating an existing rule should be possible");
        }

        service.delete(saved.id());
        try {
            service.save(rule);
        } catch (IllegalArgumentException e) {
            fail("Removing a rule should clean up the title index.");
        }
    }


    @Test
    public void loadMultiple() {

        RuleDao rule1 = service.save(RuleDao.create(null, "test1", "description", "rule \"test1\" when true then end", null, null));
        RuleDao rule2 = service.save(RuleDao.create(null, "test2", "description", "rule \"test2\" when true then end", null, null));
        RuleDao rule3 = service.save(RuleDao.create(null, "test3", "description", "rule \"test3\" when true then end", null, null));
        RuleDao rule4 = service.save(RuleDao.create(null, "test4", "description", "rule \"test4\" when true then end", null, null));

        assertThat(service.loadAll()).containsExactlyInAnyOrder(rule1, rule2, rule3, rule4);

        assertThat(service.loadNamed(ImmutableList.of("test3", "test2"))).containsExactlyInAnyOrder(rule2, rule3);
    }
}
