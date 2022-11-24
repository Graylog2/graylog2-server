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
import { useMemo } from 'react';
import type * as Immutable from 'immutable';

import { isPermitted, isAnyPermitted } from 'util/PermissionsMixin';
import { Table } from 'components/bootstrap';
import TableHead from 'components/common/ConfigurableDataTable/TableHead';
import TableBody from 'components/common/ConfigurableDataTable/TableBody';
import useCurrentUser from 'hooks/useCurrentUser';

import type { CustomCells, CustomHeaders, Attribute } from './types';

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
  /** Custom cell render for an attribute */
  customCells?: CustomCells<ListItem>,
  /** Define the permissions a user needs to view an attribute. */
  attributePermissions?: { [attributeId: string]: { permissions: Array<string>, any?: boolean } },
  /** Custom header render for an attribute */
  customHeaders?: CustomHeaders,
  /** Which attribute should be shown. */
  attributes: Array<string>,
  /** List of all available attributes. */
  availableAttributes: Array<Attribute>,
};

/**
 * Flexible data table component which allows defining custom cell renderers.
 */
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
        <TableBody rows={rows}
                   customCells={customCells}
                   rowActions={rowActions}
                   displayRowActions={displayActionsCol}
                   visibleAttributes={visibleAttributes} />
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
