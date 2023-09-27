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
package org.graylog.plugins.pipelineprocessor.rulebuilder.db;

import java.util.Collection;
import java.util.Optional;

public interface RuleFragmentService {
    RuleFragment save(RuleFragment ruleFragment);

    void delete(String name);

    Optional<RuleFragment> get(String name);

    void deleteAll();

    long count(String name);

    Collection<RuleFragment> all();

    default RuleFragment upsert(RuleFragment ruleFragment) {
        if (this.count(ruleFragment.getName()) > 0) {
            this.delete(ruleFragment.getName());
        }
        return this.save(ruleFragment);
    }

}
