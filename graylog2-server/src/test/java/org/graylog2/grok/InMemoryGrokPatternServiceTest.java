/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.grok;

import com.google.common.collect.ImmutableList;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class InMemoryGrokPatternServiceTest {

    private InMemoryGrokPatternService service;

    @Before
    public void setup() {
        service = new InMemoryGrokPatternService();
    }

    @Test
    public void load() throws Exception {
        final GrokPattern pattern = service.save(GrokPattern.create("NAME", ".*"));

        final GrokPattern loaded = service.load(pattern.id());

        assertThat(loaded)
                .isNotNull()
                .isEqualTo(pattern);

        try {
            service.load("whatever");
        } catch (NotFoundException e) {
            assertThat(e).hasMessage("Couldn't find Grok pattern with ID " + "whatever");
        }
    }

    @Test
    public void loadAll() throws Exception {
        GrokPattern pattern1 = service.save(GrokPattern.create("NAME1", ".*"));
        GrokPattern pattern2 = service.save(GrokPattern.create("NAME2", ".*"));
        GrokPattern pattern3 = service.save(GrokPattern.create("NAME3", ".*"));

        assertThat(service.loadAll()).containsExactlyInAnyOrder(pattern1, pattern2, pattern3);
    }

    @Test
    public void save() throws Exception {
        // new pattern
        final GrokPattern pattern = service.save(GrokPattern.create("NEW", ".*"));

        assertThat(pattern).isNotNull();
        assertThat(pattern.id()).isNotEmpty();

        // check that updating works
        final GrokPattern updated = service.save(pattern.toBuilder().name("OTHERNAME").build());

        final GrokPattern loaded = service.load(pattern.id());

        assertThat(loaded).isEqualTo(updated);

        // save should validate
        try {
            service.save(GrokPattern.create("INVALID", "*"));
            fail("Show throw ValidationException");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    public void saveAll() throws Exception {
        Collection<GrokPattern> patterns = ImmutableList.of(GrokPattern.create("1", ".*"),
                                                            GrokPattern.create("2", ".+"));
        final List<GrokPattern> saved = service.saveAll(patterns, false);
        assertThat(saved).hasSize(2);

        service.saveAll(patterns, false);
        // should have added the patterns again
        assertThat(service.loadAll()).hasSize(4);

        // replaced all patterns
        service.saveAll(patterns, true);
        assertThat(service.loadAll()).hasSize(2);
    }

    @Test
    public void delete() throws Exception {
        final GrokPattern saved = service.save(GrokPattern.create("NAME", "name"));
        try {
            service.load(saved.id());
        } catch (Exception e) {
            fail("Should not throw");
        }
        assertThat(service.delete(saved.id())).isEqualTo(1);

        try {
            service.load(saved.id());
            fail("Should throw");
        } catch (NotFoundException ignored) {
        } catch (Exception e) {
            fail("Should not throw any other exceptions");
        }
    }

    @Test
    public void deleteAll() throws Exception {
        Collection<GrokPattern> patterns = ImmutableList.of(GrokPattern.create("1", ".*"),
                                                            GrokPattern.create("2", ".+"));
        final List<GrokPattern> saved = service.saveAll(patterns, false);
        assertThat(service.deleteAll()).isEqualTo(2);
        assertThat(service.loadAll()).isEmpty();
    }

}