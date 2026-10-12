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
import AppConfig from 'util/AppConfig';

import type { ExtensionColumnGroups } from './hooks/useIndexSetsOverviewExtensions';
import { METRIC_COLUMN_IDS, METRIC_COLUMN_TITLES } from './metricColumns';
import type { IndexSetCategory } from './types';

export const INDEX_SET_VIEW_VARIANTS = {
  default: '' as const,
  configuration: 'configuration' as const,
  routing: 'routing' as const,
};

export const DETAILS_SECTION = 'details';

// Hidden until BE-4: the backend reports the worst deflector health of all writable sets for each of them.
const DISPLAYED_METRIC_COLUMN_IDS = Object.values(METRIC_COLUMN_IDS).filter(
  (id) => id !== METRIC_COLUMN_IDS.deflectorHealth,
);

const CLOUD_HIDDEN_ATTRIBUTES = ['shards', 'replicas'];

export const filterCloudHiddenAttributes = <T extends string | { id: string }>(items: Array<T>): Array<T> =>
  AppConfig.isCloud()
    ? items.filter((item) => !CLOUD_HIDDEN_ATTRIBUTES.includes(typeof item === 'string' ? item : item.id))
    : items;

export const CATEGORY_ATTRIBUTE = 'category';

export const INDEX_SET_CATEGORY_TITLES: Record<IndexSetCategory, string> = {
  user: 'User-defined',
  illuminate: 'Illuminate',
  system: 'System',
};

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

  const defaultCols = [
    'title',
    'description',
    'index_prefix',
    'index_template_type',
    METRIC_COLUMN_IDS.indexCount,
    METRIC_COLUMN_IDS.sizeBytes,
    'creation_date',
  ];
  const configurationCols = filterCloudHiddenAttributes([
    'title',
    'shards',
    'replicas',
    'field_type_refresh_interval',
    'field_type_profile',
    METRIC_COLUMN_IDS.fieldCount,
    ...extensionColumnGroups.configuration,
  ]);

  const defaultColumnOrder = filterCloudHiddenAttributes([
    'title',
    'description',
    'index_prefix',
    'index_template_type',
    CATEGORY_ATTRIBUTE,
    'stream_count',
    METRIC_COLUMN_IDS.indexCount,
    METRIC_COLUMN_IDS.documentCount,
    METRIC_COLUMN_IDS.sizeBytes,
    'shards',
    'replicas',
    'field_type_refresh_interval',
    'field_type_profile',
    METRIC_COLUMN_IDS.fieldCount,
    ...extensionColumnGroups.configuration,
    ...ungroupedExtensionIds,
    'creation_date',
  ]);

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

  const routingVariantLayout = {
    ...SHARED_LAYOUT,
    layoutVariant: INDEX_SET_VIEW_VARIANTS.routing,
    defaultColumnOrder,
    defaultDisplayedAttributes: ['title', 'index_prefix', 'stream_count'],
  };

  const additionalAttributes: Array<Attribute> = [
    { id: 'field_type_refresh_interval', title: 'Field Type Refresh Interval', sortable: false },
    ...DISPLAYED_METRIC_COLUMN_IDS.map((id) => ({ id, title: METRIC_COLUMN_TITLES[id], sortable: false })),
    ...extensionAttributes,
  ];

  return {
    defaultVariantLayout,
    configurationVariantLayout,
    routingVariantLayout,
    additionalAttributes,
  };
};

export default getIndexSetTableElements;
