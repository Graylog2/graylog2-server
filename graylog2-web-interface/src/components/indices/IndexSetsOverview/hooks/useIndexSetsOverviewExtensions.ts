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
import usePluginEntities from 'hooks/usePluginEntities';
import type { Attribute } from 'stores/PaginationTypes';
import type { ColumnRenderersByAttribute } from 'components/common/EntityDataTable/types';
import type { IndexSetEntity } from 'components/indices/IndexSetsOverview/types';

export type ExtensionColumnGroups = {
  configuration: Array<string>;
};

type TableElement = { attributes?: Array<Attribute>; backendAttributeIds?: Array<string> };

const columnIdsOf = ({ attributes, backendAttributeIds }: TableElement): Array<string> => [
  ...(attributes ?? []).map(({ id }) => id),
  ...(backendAttributeIds ?? []),
];

const useIndexSetsOverviewExtensions = (): {
  columnRenderers: ColumnRenderersByAttribute<IndexSetEntity>;
  attributes: Array<Attribute>;
  columnIds: Array<string>;
  columnGroups: ExtensionColumnGroups;
} => {
  const tableElements = usePluginEntities('components.indexSets.overview.tableElements');

  return {
    columnRenderers: Object.fromEntries(
      tableElements.flatMap(({ columnRenderers }) => Object.entries(columnRenderers ?? {})),
    ),
    attributes: tableElements.flatMap(({ attributes }) => attributes ?? []),
    columnIds: tableElements.flatMap(columnIdsOf),
    columnGroups: {
      configuration: tableElements.filter(({ group }) => group === 'configuration').flatMap(columnIdsOf),
    },
  };
};

export default useIndexSetsOverviewExtensions;
