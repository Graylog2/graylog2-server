/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.db.memory;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog2.database.NotFoundException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class InMemoryRuleServiceTest {
    private InMemoryRuleService service;

    @Before
    public void setup() {
        service = new InMemoryRuleService();
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
            service.save(saved.toBuilder().createdAt(DateTime.now()).build());
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