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
import { SHARED_WITH_OTHERS_ATTRIBUTE } from 'components/permissions/SharedWithOthers/Constants';

const getDashboardTableElements = (pluggableAttributes?: {
  attributeNames?: Array<string>;
  attributes?: Array<Attribute>;
}) => {
  const getDefaultLayout = (hideAdditionalColumns: boolean) => ({
    entityTableId: 'dashboards',
    defaultPageSize: 20,
    defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
    defaultDisplayedAttributes: hideAdditionalColumns
      ? ['title', 'description', 'summary']
      : ['title', 'description', 'summary', 'favorite'],
    defaultColumnOrder: [
      'title',
      'summary',
      'description',
      'owner',
      SHARED_WITH_OTHERS_ATTRIBUTE.id,
      '_entity_source.source',
      'created_at',
      'last_updated_at',
      ...(pluggableAttributes?.attributeNames || []),
      'favorite',
    ],
  });

  const additionalAttributes = [...(pluggableAttributes?.attributes || []), SHARED_WITH_OTHERS_ATTRIBUTE];

  return {
    getDefaultLayout,
    additionalAttributes,
  };
};

export default getDashboardTableElements;
