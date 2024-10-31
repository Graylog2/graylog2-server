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

import type { IndexSet } from 'stores/indices/IndexSetsStore';

// eslint-disable-next-line import/prefer-default-export
export const indexSets: Array<IndexSet> = [
  {
    id: 'index-set-id-1',
    title: 'Default index set',
    description: 'The Graylog default index set',
    can_be_default: true,
    index_prefix: 'graylog',
    shards: 4,
    replicas: 0,
    rotation_strategy_class: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy',
    rotation_strategy: {
      type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig',
      max_docs_per_index: 20000000,
    },
    retention_strategy_class: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy',
    retention_strategy: {
      type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig',
      max_number_of_indices: 20,
    },
    creation_date: '2022-01-01T10:55:10.000Z',
    index_analyzer: 'standard',
    index_optimization_max_num_segments: 1,
    index_optimization_disabled: false,
    field_type_refresh_interval: 5000,
    field_type_profile: null,
    index_template_type: null,
    default: true,
    writable: true,
  },
  {
    id: 'index-set-id-2',
    title: 'Example Index Set',
    description: 'Example Index Set Description',
    can_be_default: true,
    index_prefix: 'example',
    shards: 4,
    replicas: 0,
    rotation_strategy_class: 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy',
    rotation_strategy: {
      type: 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig',
      rotation_period: 'P1D',
      max_rotation_period: null,
      rotate_empty_index_set: false,
    },
    retention_strategy_class: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy',
    retention_strategy: {
      type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig',
      max_number_of_indices: 20,
    },
    creation_date: '2022-01-01T10:55:10.000Z',
    index_analyzer: 'standard',
    index_optimization_max_num_segments: 1,
    index_optimization_disabled: false,
    field_type_refresh_interval: 5000,
    field_type_profile: null,
    index_template_type: null,
    default: false,
    writable: true,
  },
];
