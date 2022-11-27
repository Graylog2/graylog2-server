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
import TableHead from 'components/common/EntityDataTable/TableHead';
import TableRow from 'components/common/EntityDataTable/TableRow';
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

type Props<Entity extends { id: string }> = {
  /** Currently active sort */
  activeSort?: Sort,
  /** Define the permissions a user needs to view an attribute. */
  attributePermissions?: { [attributeId: string]: { permissions: Array<string>, any?: boolean } },
  /** Which attribute should be shown. */
  attributes: Array<string>,
  /** List of all available attributes. */
  availableAttributes: Array<Attribute>,
  /** Supported batch operations */
  bulkActions?: (selectedItemsIds: Array<string>, setSelectedItemsIds: (streamIds: Array<string>) => void) => React.ReactNode
  /** Custom cell render for an attribute */
  customCells?: CustomCells<Entity>,
  /** Custom header render for an attribute */
  customHeaders?: CustomHeaders,
  /** The table data. */
  data: Array<Entity>,
  /** Function to handle sort changes */
  onSortChange: (newSort: Sort) => void,
  /** Actions for each row. */
  rowActions?: (entity: Entity) => React.ReactNode,
  /** Total amount of items */
  total: number,
};

/**
 * Flexible data table component which allows defining custom cell renderers.
 */
const EntityDataTable = <Entity extends { id: string }>({
  activeSort,
  attributePermissions,
  attributes,
  availableAttributes,
  bulkActions,
  customCells,
  customHeaders,
  onSortChange,
  rowActions,
  data,
  total,
}: Props<Entity>) => {
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
  const displayBulkActionsCol = typeof bulkActions === 'function';

  return (
    <ScrollContainer>
      <ActionsRow>
        <div>
          {(displayBulkActionsCol && !!selectedItemsIds?.length) && (
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
                   selectedItemsIds={selectedItemsIds}
                   setSelectedItemsIds={setSelectedItemsIds}
                   data={data}
                   customHeaders={customHeaders}
                   onSortChange={onSortChange}
                   displayBulkActionsCol={displayBulkActionsCol}
                   activeSort={activeSort}
                   displayActionsCol={displayActionsCol} />
        <tbody>
          {data.map((entity) => (
            <TableRow entity={entity}
                      key={entity.id}
                      onToggleRowSelect={onToggleRowSelect}
                      customCells={customCells}
                      isSelected={!!selectedItemsIds?.includes(entity.id)}
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

EntityDataTable.defaultProps = {
  activeSort: undefined,
  attributePermissions: undefined,
  bulkActions: undefined,
  customCells: undefined,
  customHeaders: undefined,
  rowActions: undefined,
};

export default EntityDataTable;
