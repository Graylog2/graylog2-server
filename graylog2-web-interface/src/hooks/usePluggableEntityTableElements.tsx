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

import usePluginEntities from 'hooks/usePluginEntities';
import type { EntityBase } from 'components/common/EntityDataTable/types';

const usePluggableEntityTableElements = <T extends EntityBase>(_entity: T, entityType: string) => {
  const pluginTableElements = usePluginEntities('components.shared.entityTableElements');

  const tableElements = pluginTableElements.filter((action) => (action.useCondition ? !!action.useCondition() : true));
  const pluggableColumnRenderers = tableElements.reduce(
    (acc, curr) => ({ ...acc, ...curr.getColumnRenderer(entityType) }),
    {},
  );
  const pluggableAttributes = tableElements.reduce((acc, curr) => [...acc, ...curr.attributes], []);
  const pluggableAttributeNames = tableElements.reduce((acc, curr) => [...acc, curr.attributeName], []);
  const pluggableExpandedSections = tableElements.reduce(
    (acc, curr) => ({ ...acc, ...curr.expandedSection(entityType) }),
    {},
  );

  const getPluggableTableCells = (entityId: string) =>
    tableElements
      .reduce(
        (acc, curr) => [
          ...acc,
          {
            component: curr.tableCellComponent,
          },
        ],
        [],
      )
      .map(({ component: PluggableTableCell }) => (
        <PluggableTableCell key={entityId} entityId={entityId} entityType={entityType} />
      ));

  const pluggableTableHeaders = tableElements
    .reduce((acc, curr) => [...acc, ...curr.attributes], [])
    .map((attribute) => (
      <th key={attribute.id} className={`entity-table-header-${attribute.id}`}>
        {attribute.title}
      </th>
    ));

  return {
    pluggableColumnRenderers,
    pluggableAttributes: {
      attributeNames: pluggableAttributeNames,
      attributes: pluggableAttributes,
    },
    pluggableExpandedSections,
    getPluggableTableCells,
    pluggableTableHeaders,
  };
};

export default usePluggableEntityTableElements;
