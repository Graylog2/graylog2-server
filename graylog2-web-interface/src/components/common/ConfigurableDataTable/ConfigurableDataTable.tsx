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

import { Button, Table, ButtonToolbar } from 'components/bootstrap';
import { isPermitted, isAnyPermitted } from 'util/PermissionsMixin';
import TableHead from 'components/common/ConfigurableDataTable/TableHead';
import TableRow from 'components/common/ConfigurableDataTable/TableRow';
import useCurrentUser from 'hooks/useCurrentUser';
import StringUtils from 'util/StringUtils';

import type { CustomCells, CustomHeaders, Attribute, Sort } from './types';

const ScrollContainer = styled.div(({ theme }) => css`
  @media (max-width: ${theme.breakpoints.max.md}) {
    overflow-x: auto;
  }
`);

const ActionsRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  min-height: 22px;
`;

const BulkActionsWrapper = styled.div`
  display: flex;
  align-items: center;
`;

const BulkActions = styled(ButtonToolbar)`
  margin-left: 5px;
`;

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
  bulkActions?: (selectedItemsIds: Array<string>, setSelectedItemsIds: (streamIds: Array<string>) => void) => React.ReactNode
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
  /** Total amount of items */
  total: number,
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
  bulkActions,
  customHeaders,
  onSortChange,
  rowActions,
  rows,
  total,
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

  const unselectAllItems = useCallback(() => setSelectedItemsIds([]), []);
  const displayActionsCol = typeof rowActions === 'function';
  const displayBulkActionsCol = !!bulkActions?.length;

  return (
    <ScrollContainer>
      <ActionsRow>
        <div>
          {(!!bulkActions?.length && !!selectedItemsIds?.length) && (
            <BulkActionsWrapper>
              {selectedItemsIds.length} {StringUtils.pluralize(selectedItemsIds.length, 'item', 'items')} selected
              <BulkActions>
                {bulkActions(selectedItemsIds, setSelectedItemsIds)}
                <Button bsSize="xsmall" onClick={unselectAllItems}>Cancel</Button>
              </BulkActions>
            </BulkActionsWrapper>
          )}
        </div>
        <div>
          Total {total}
        </div>
      </ActionsRow>
      <Table striped condensed hover>
        <TableHead selectedAttributes={visibleAttributes}
                   customHeaders={customHeaders}
                   onSortChange={onSortChange}
                   displayBulkActionsCol={displayBulkActionsCol}
                   activeSort={activeSort}
                   displayActionsCol={displayActionsCol} />
        <tbody>
          {rows.map((listItem) => (
            <TableRow listItem={listItem}
                      key={listItem.id}
                      onToggleRowSelect={onToggleRowSelect}
                      customCells={customCells}
                      selectedItemsIds={selectedItemsIds}
                      rowActions={rowActions}
                      displayBulkActionsCol={displayBulkActionsCol}
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
  bulkActions: undefined,
  customCells: undefined,
  customHeaders: undefined,
  rowActions: undefined,
};

export default ConfigurableDataTable;
