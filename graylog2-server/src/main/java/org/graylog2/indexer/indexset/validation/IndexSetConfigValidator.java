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
package org.graylog2.indexer.indexset.validation;

import org.graylog2.indexer.indexset.fields.RotationAndRetentionFields;
import org.graylog2.indexer.indexset.validation.IndexSetValidator.Violation;

import java.util.Optional;

/**
 * Extension point for plugins that need to reject an index set or index set template configuration.
 * <p>
 * Implementations are added to the {@code Set<IndexSetConfigValidator>} multibinding and are called by
 * {@link IndexSetValidator} after the built-in checks have passed. The first violation returned wins.
 * <p>
 * The parameter is the common super type of {@link org.graylog2.indexer.indexset.IndexSetConfig} and
 * {@link org.graylog2.indexer.indexset.template.IndexSetTemplateConfig}, so a single implementation covers
 * index sets and templates alike.
 */
public interface IndexSetConfigValidator {

    Optional<Violation> validate(RotationAndRetentionFields config);
}
