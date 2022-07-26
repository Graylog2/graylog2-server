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
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import IndexSetConfigurationForm from './IndexSetConfigurationForm';

const indexSet = {
  id: '62665eb0526719678ed3719f',
  title: 'anotyher',
  description: 'test',
  can_be_default: true,
  index_prefix: 'another',
  shards: 4,
  replicas: 0,
  rotation_strategy_class: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy',
  rotation_strategy: {
    type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig',
    max_docs_per_index: 20000000,
  },
  retention_strategy_class: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategy',
  retention_strategy: {
    type: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig',
    max_number_of_indices: 2147483647,
  },
  creation_date: '2022-04-25T08:41:20.497Z',
  index_analyzer: 'standard',
  index_optimization_max_num_segments: 1,
  index_optimization_disabled: true,
  field_type_refresh_interval: 5000,
  index_template_type: null,
  writable: true,
  default: false,
};
const retentionStrategies = [
  {
    type: 'org.graylog.plugins.archive.indexer.retention.strategies.ArchiveRetentionStrategy',
    default_config: {
      type: 'org.graylog.plugins.archive.indexer.retention.strategies.ArchiveRetentionStrategyConfig',
      max_number_of_indices: 20,
      index_action: 'CLOSE',
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog:plugins:archive:indexer:retention:strategies:ArchiveRetentionStrategyConfig',
      properties: {
        max_number_of_indices: {
          type: 'integer',
        },
        index_action: {
          type: 'string',
          enum: [
            'NONE',
            'CLOSE',
            'DELETE',
          ],
        },
        type: {
          type: 'string',
        },
      },
    },
  },
  {
    type: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategy',
    default_config: {
      type: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig',
      max_number_of_indices: 2147483647,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:retention:strategies:NoopRetentionStrategyConfig',
      properties: {
        max_number_of_indices: {
          type: 'integer',
        },
        type: {
          type: 'string',
        },
      },
    },
  },
  {
    type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy',
    default_config: {
      type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig',
      max_number_of_indices: 20,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:retention:strategies:DeletionRetentionStrategyConfig',
      properties: {
        max_number_of_indices: {
          type: 'integer',
        },
        type: {
          type: 'string',
        },
      },
    },
  },
  {
    type: 'org.graylog2.indexer.retention.strategies.ClosingRetentionStrategy',
    default_config: {
      type: 'org.graylog2.indexer.retention.strategies.ClosingRetentionStrategyConfig',
      max_number_of_indices: 20,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:retention:strategies:ClosingRetentionStrategyConfig',
      properties: {
        max_number_of_indices: {
          type: 'integer',
        },
        type: {
          type: 'string',
        },
      },
    },
  },
];
const retentionStrategiesContext = {
  max_index_retention_period: 'P1D',
};
const rotationStrategies = [
  {
    type: 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy',
    default_config: {
      type: 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig',
      rotation_period: 'P1D',
      max_rotation_period: null,
      rotate_empty_index_sets: false,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:rotation:strategies:TimeBasedRotationStrategyConfig',
      properties: {
        rotation_period: {
          type: 'string',
        },
        max_rotation_period: {
          type: 'string',
        },
        rotate_empty_index_sets: {
          type: 'boolean',
        },
        type: {
          type: 'string',
        },
      },
    },
  },
  {
    type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy',
    default_config: {
      type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig',
      max_docs_per_index: 20000000,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:rotation:strategies:MessageCountRotationStrategyConfig',
      properties: {
        max_docs_per_index: {
          type: 'integer',
        },
        type: {
          type: 'string',
        },
      },
    },
  },
  {
    type: 'org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy',
    default_config: {
      type: 'org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategyConfig',
      max_size: 1073741824,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:rotation:strategies:SizeBasedRotationStrategyConfig',
      properties: {
        type: {
          type: 'string',
        },
        max_size: {
          type: 'integer',
        },
      },
    },
  },
];

describe('IndexSetConfigurationForm', () => {
  const onSave = jest.fn();
  const cancelLink = '/cancelLink';

  it('Should render IndexSetConfigurationForm', () => {
    render(<IndexSetConfigurationForm indexSet={indexSet}
                                      retentionStrategiesContext={retentionStrategiesContext}
                                      rotationStrategies={rotationStrategies}
                                      retentionStrategies={retentionStrategies}
                                      cancelLink={cancelLink}
                                      onUpdate={onSave} />);

    const titleText = screen.getByText(/title/i);

    expect(titleText).toBeInTheDocument();
  });

  it('Should render create IndexSetConfigurationForm', () => {
    render(<IndexSetConfigurationForm indexSet={indexSet}
                                      retentionStrategiesContext={retentionStrategiesContext}
                                      rotationStrategies={rotationStrategies}
                                      retentionStrategies={retentionStrategies}
                                      create
                                      cancelLink={cancelLink}
                                      onUpdate={onSave} />);

    const indexPrefix = screen.getByText(/index prefix/i);

    expect(indexPrefix).toBeInTheDocument();
  });
});
