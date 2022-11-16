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
import styled, { css } from 'styled-components';
import { useMemo, useState, useCallback } from 'react';
import type * as Immutable from 'immutable';
import { Badge } from 'react-bootstrap';

import { isPermitted, isAnyPermitted } from 'util/PermissionsMixin';
import { Table } from 'components/bootstrap';
import TableHead from 'components/common/ConfigurableDataTable/TableHead';
import TableRow from 'components/common/ConfigurableDataTable/TableRow';
import useCurrentUser from 'hooks/useCurrentUser';

import type { CustomCells, CustomHeaders, Attribute, Sort } from './types';

const ScrollContainer = styled.div(({ theme }) => css`
  @media (max-width: ${theme.breakpoints.max.md}) {
    overflow-x: auto;
  }
`);

const filterVisibleAttributes = (
  attributes: Array<string>,
  availableAttributes: Array<Attribute>,
  attributePermissions: { [attributeId: string]: { permissions: Array<string>, any?: boolean } },
  userPermissions: Immutable.List<string>,
) => attributes
  .map((attributeId) => availableAttributes.find(({ id }) => id === attributeId))
  .filter(({ id }) => {
    if (attributePermissions?.[id]) {
      const { permissions, any } = attributePermissions[id];

      return any
        ? isAnyPermitted(userPermissions, permissions)
        : isPermitted(userPermissions, permissions);
    }

    return true;
  });

type Props<ListItem extends { id: string }> = {
  /** The table list items. */
  rows: Array<ListItem>,
  /** Actions for each row. */
  rowActions?: (listItem: ListItem) => React.ReactNode,
  /** Suported batch operations */
  batchActions?: (selectedItemsIds: Array<string>) => React.ReactNode
  /** Custom cell render for an attribute */
  customCells?: CustomCells<ListItem>,
  /** Define the permissions a user needs to view an attribute. */
  attributePermissions?: { [attributeId: string]: { permissions: Array<string>, any?: boolean } },
  /** Custom header render for an attribute */
  customHeaders?: CustomHeaders,
  /** Function to handle sort changes */
  onSortChange: (newSort: Sort) => void
  /** Currently active sort */
  activeSort?: Sort,
  /** Which attribute should be shown. */
  attributes: Array<string>,
  /** List of all available attributes. */
  availableAttributes: Array<Attribute>,
};

/**
 * Flexible data table component which allows defining custom cell renderers.
 */
const ConfigurableDataTable = <ListItem extends { id: string }>({
  activeSort,
  attributePermissions,
  attributes,
  availableAttributes,
  customCells,
  batchActions,
  customHeaders,
  onSortChange,
  rowActions,
  rows,
}: Props<ListItem>) => {
  const [selectedItemsIds, setSelectedItemsIds] = useState<Array<string>>([]);
  const currentUser = useCurrentUser();
  const visibleAttributes = useMemo(
    () => filterVisibleAttributes(attributes, availableAttributes, attributePermissions, currentUser.permissions),
    [attributePermissions, attributes, availableAttributes, currentUser.permissions],
  );
  const onToggleRowSelect = useCallback((itemId: string) => {
    setSelectedItemsIds(((cur) => {
      if (cur.includes(itemId)) {
        return cur.filter((id) => id !== itemId);
      }

      return [...cur, itemId];
    }));
  }, []);

  const displayActionsCol = typeof rowActions === 'function';
  const displayBatchSelectCol = !!batchActions?.length;

  return (
    <ScrollContainer>
      {!!batchActions?.length && !!selectedItemsIds?.length && <div>Selected: <Badge>{selectedItemsIds?.length}</Badge></div>}
      <Table striped condensed hover>
        <TableHead selectedAttributes={visibleAttributes}
                   customHeaders={customHeaders}
                   onSortChange={onSortChange}
                   displayBatchSelectCol={displayBatchSelectCol}
                   activeSort={activeSort}
                   displayActionsCol={displayActionsCol} />
        <tbody>
          {rows.map((listItem) => (
            <TableRow listItem={listItem}
                      onToggleRowSelect={onToggleRowSelect}
                      customCells={customCells}
                      selectedItemsIds={selectedItemsIds}
                      rowActions={rowActions}
                      displayBatchSelectCol={displayBatchSelectCol}
                      displayRowActions={displayActionsCol}
                      visibleAttributes={visibleAttributes} />
          ))}
        </tbody>
      </Table>
    </ScrollContainer>
  );
};

ConfigurableDataTable.defaultProps = {
  activeSort: undefined,
  attributePermissions: undefined,
  batchActions: undefined,
  customCells: undefined,
  customHeaders: undefined,
  rowActions: undefined,
};

export default ConfigurableDataTable;
