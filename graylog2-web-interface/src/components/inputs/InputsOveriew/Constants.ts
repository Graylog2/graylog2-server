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
import type { Sort } from 'stores/PaginationTypes';

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
      'node_id',
      'address',
      'port',
    ],
    defaultColumnOrder: ['title', 'type', 'direction', 'desired_state', 'traffic', 'node_id', 'address', 'port'],
  };

  const additionalAttributes = [
    { id: 'traffic', title: 'Traffic Last Minute' },
    { id: 'address', title: 'Address' },
    { id: 'port', title: 'Port' },
  ];

  return {
    tableLayout,
    additionalAttributes,
  };
};

export default getInputsTableElements;
