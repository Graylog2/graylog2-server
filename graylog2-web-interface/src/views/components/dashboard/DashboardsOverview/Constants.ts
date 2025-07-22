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

const getDashboardTableElements = (pluggableAttributes?: {
  attributeNames?: Array<string>;
  attributes?: Array<Attribute>;
}) => {
  const getDefaultLayout = (isEvidenceModal: boolean) => ({
    entityTableId: 'dashboards',
    defaultPageSize: 20,
    defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
    defaultDisplayedAttributes: isEvidenceModal
      ? ['title', 'description', 'summary']
      : ['title', 'description', 'summary', 'favorite', ...(pluggableAttributes?.attributeNames || [])],
  });

  const columnOrder = [
    'title',
    'summary',
    'description',
    'owner',
    'created_at',
    'last_updated_at',
    'favorite',
    ...(pluggableAttributes?.attributeNames || []),
  ];

  const additionalAttributes = [...(pluggableAttributes?.attributes || [])];

  return {
    getDefaultLayout,
    columnOrder,
    additionalAttributes,
  };
};

export default getDashboardTableElements;
