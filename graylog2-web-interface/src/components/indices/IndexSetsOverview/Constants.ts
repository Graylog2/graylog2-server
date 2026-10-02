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

import type { ExtensionColumnGroups } from './hooks/useIndexSetsOverviewExtensions';

export const INDEX_SET_VIEW_VARIANTS = {
  default: '' as const,
  configuration: 'configuration' as const,
};

export const DETAILS_SECTION = 'details';

const SHARED_LAYOUT = {
  entityTableId: 'index-sets',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
};

const getIndexSetTableElements = (
  extensionAttributes: Array<Attribute>,
  extensionColumnGroups: ExtensionColumnGroups,
) => {
  const groupedIds = new Set(extensionColumnGroups.configuration);
  const ungroupedExtensionIds = extensionAttributes.map(({ id }) => id).filter((id) => !groupedIds.has(id));

  const defaultCols = ['title', 'description', 'index_prefix', 'index_template_type', 'creation_date'];
  const configurationCols = [
    'title',
    'shards',
    'replicas',
    'field_type_refresh_interval',
    'field_type_profile',
    ...extensionColumnGroups.configuration,
  ];

  const defaultColumnOrder = [
    'title',
    'description',
    'index_prefix',
    'index_template_type',
    'shards',
    'replicas',
    'field_type_refresh_interval',
    'field_type_profile',
    ...extensionColumnGroups.configuration,
    ...ungroupedExtensionIds,
    'creation_date',
  ];

  const defaultVariantLayout = {
    ...SHARED_LAYOUT,
    defaultColumnOrder,
    defaultDisplayedAttributes: defaultCols,
  };

  const configurationVariantLayout = {
    ...SHARED_LAYOUT,
    layoutVariant: INDEX_SET_VIEW_VARIANTS.configuration,
    defaultColumnOrder,
    defaultDisplayedAttributes: configurationCols,
  };

  const additionalAttributes: Array<Attribute> = [
    { id: 'field_type_refresh_interval', title: 'Field Type Refresh Interval', sortable: false },
    { id: 'field_type_profile', title: 'Field Type Profile', sortable: false },
    ...extensionAttributes,
  ];

  return {
    defaultVariantLayout,
    configurationVariantLayout,
    additionalAttributes,
  };
};

export default getIndexSetTableElements;
