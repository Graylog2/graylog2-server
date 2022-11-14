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
import styled from 'styled-components';
import { useMemo } from 'react';
import type * as Immutable from 'immutable';

import { isPermitted, isAnyPermitted } from 'util/PermissionsMixin';
import { Table } from 'components/bootstrap';
import { TextOverflowEllipsis } from 'components/common/index';
import TableHead from 'components/common/ConfigurableDataTable/TableHead';
import useCurrentUser from 'hooks/useCurrentUser';

export type Attribute = {
  id: string,
  title: string,
  type?: boolean,
};

export type CustomHeaders = { [key: string]: (attribute: Attribute) => React.ReactNode }
export type CustomCells<ListItem extends { id: string }> = {
  [key: string]: {
    renderCell: (listItem: ListItem, attribute: Attribute, key: string) => React.ReactNode,
    width?: string,
    maxWidth?: string,
  }
}

const ScrollContainer = styled.div`
  overflow-x: auto;
`;

const ActionsCell = styled.td`
  > div {
    display: flex;
    justify-content: right;
  }
`;

const attributeCellRenderer = {
  description: {
    renderCell: (listItem) => (
      <TextOverflowEllipsis>
        {listItem.description}
      </TextOverflowEllipsis>
    ),
    maxWidth: '30vw',
  },
};

type Props<ListItem extends { id: string }> = {
  rows: Array<ListItem>,
  rowActions?: (listItem: ListItem) => React.ReactNode,
  customCells?: CustomCells<ListItem>,
  attributePermissions?: { [attributeId: string]: { permissions: Array<string>, any?: boolean } },
  customHeaders?: CustomHeaders,
  attributes: Array<string>,
  availableAttributes: Array<Attribute>,
};

const filterVisibleAttributes = (
  attributes: Array<string>,
  availableAttributes: Array<Attribute>,
  attributePermissions: { [attributeId: string]: { permissions: Array<string>, any?: boolean } },
  userPermissions: Immutable.List<string>,
) => {
  return attributes
    .map((attributeId) => availableAttributes.find(({ id }) => id === attributeId))
    .filter(({ id }) => {
      if (attributePermissions?.[id]) {
        const { permissions, any } = attributePermissions[id];

        if (any) {
          return isAnyPermitted(userPermissions, permissions);
        }

        return isPermitted(userPermissions, permissions);
      }

      return true;
    },
    );
};

const ConfigurableDataTable = <ListItem extends { id: string }>({
  rows,
  customHeaders,
  customCells,
  attributes,
  availableAttributes,
  attributePermissions,
  rowActions,
}: Props<ListItem>) => {
  const currentUser = useCurrentUser();
  const visibleAttributes = useMemo(
    () => filterVisibleAttributes(attributes, availableAttributes, attributePermissions, currentUser.permissions),
    [attributePermissions, attributes, availableAttributes, currentUser.permissions],
  );

  const displayActionsCol = typeof rowActions === 'function';

  return (
    <ScrollContainer>
      <Table striped condensed hover>
        <TableHead selectedAttributes={visibleAttributes}
                   customHeaders={customHeaders}
                   displayActionsCol={displayActionsCol} />
        <tbody>
          {rows.map((listItem) => (
            <tr key={listItem.id}>
              {visibleAttributes.map((attribute) => {
                const cellKey = `${listItem.id}-${attribute.id}`;
                const cellRenderer = customCells?.[attribute.id] ?? attributeCellRenderer[attribute.id];

                return (
                  <td key={cellKey} style={{ width: cellRenderer.width, maxWidth: cellRenderer.maxWidth }}>
                    {cellRenderer ? cellRenderer.renderCell(listItem, attribute) : listItem[attribute.id]}
                  </td>
                );
              })}
              {displayActionsCol ? <ActionsCell>{rowActions(listItem)}</ActionsCell> : null}
            </tr>
          ))}
        </tbody>
      </Table>
    </ScrollContainer>
  );
};

ConfigurableDataTable.defaultProps = {
  attributePermissions: undefined,
  customCells: undefined,
  customHeaders: undefined,
  rowActions: undefined,
};

export default ConfigurableDataTable;
