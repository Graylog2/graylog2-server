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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog2.database.NotFoundException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * A RuleService that does not persist any data, but simply keeps it in memory.
 */
public class InMemoryRuleService implements RuleService {

    // poor man's id generator
    private AtomicLong idGen = new AtomicLong(0);

    private Map<String, RuleDao> store = new MapMaker().makeMap();
    private Map<String, String> titleToId = new MapMaker().makeMap();

    @Override
    public RuleDao save(RuleDao rule) {
        RuleDao toSave = rule.id() != null
                ? rule
                : rule.toBuilder().id(createId()).build();
        // enforce the title unique constraint
        if (titleToId.containsKey(toSave.title())) {
            // if this is an update and the title belongs to the passed rule, then it's fine
            if (!titleToId.get(toSave.title()).equals(toSave.id())) {
                throw new IllegalArgumentException("Duplicate rule titles are not allowed: " + toSave.title());
            }
        }
        titleToId.put(toSave.title(), toSave.id());
        store.put(toSave.id(), toSave);

        return toSave;
    }

    @Override
    public RuleDao load(String id) throws NotFoundException {
        final RuleDao rule = store.get(id);
        if (rule == null) {
            throw new NotFoundException("No such rule with id " + id);
        }
        return rule;
    }

    @Override
    public Collection<RuleDao> loadAll() {
        return ImmutableSet.copyOf(store.values());
    }

    @Override
    public void delete(String id) {
        if (id == null) {
            return;
        }
        final RuleDao removed = store.remove(id);
        // clean up title index if the rule existed
        if (removed != null) {
            titleToId.remove(removed.title());
        }
    }

    @Override
    public Collection<RuleDao> loadNamed(Collection<String> ruleNames) {
        final Set<String> needles = Sets.newHashSet(ruleNames);
        return store.values().stream()
                .filter(ruleDao -> needles.contains(ruleDao.title()))
                .collect(Collectors.toList());
    }

    private String createId() {
        return String.valueOf(idGen.incrementAndGet());
    }
}
