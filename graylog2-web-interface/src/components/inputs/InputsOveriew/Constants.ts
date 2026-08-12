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
import type { Attribute, Sort } from 'stores/PaginationTypes';
import { METRIC_COLUMN_IDS, METRIC_COLUMN_TITLES } from 'components/inputs/InputsOveriew/metricColumns';

const getInputsTableElements = () => {
  const tableLayout = {
    entityTableId: 'inputs',
    defaultPageSize: 50,
    defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
    defaultDisplayedAttributes: [
      'title',
      'type',
      'direction',
      'desired_state',
      'traffic',
      'input_failures',
      'node_id',
      'address',
      'port',
    ],
    defaultColumnOrder: [
      'title',
      'type',
      'direction',
      'desired_state',
      'traffic',
      'input_failures',
      METRIC_COLUMN_IDS.messagesPerStream,
      METRIC_COLUMN_IDS.extractorCount,
      METRIC_COLUMN_IDS.associatedStreams,
      'node_id',
      'address',
      'port',
    ],
  };

  const additionalAttributes: Array<Attribute> = [
    { id: 'traffic', title: 'Traffic Last Minute' },
    { id: 'input_failures', title: 'Input Failures' },
    { id: 'address', title: 'Address' },
    { id: 'port', title: 'Port', type: 'INT' },
    {
      id: METRIC_COLUMN_IDS.messagesPerStream,
      title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.messagesPerStream],
    },
    {
      id: METRIC_COLUMN_IDS.extractorCount,
      title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.extractorCount],
      type: 'INT',
    },
    {
      id: METRIC_COLUMN_IDS.associatedStreams,
      title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.associatedStreams],
    },
  ];

  return {
    tableLayout,
    additionalAttributes,
  };
};

export default getInputsTableElements;
