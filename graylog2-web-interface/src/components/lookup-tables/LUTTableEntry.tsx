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
import { useHistory } from 'react-router-dom';
import styled from 'styled-components';

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import { Button } from 'components/bootstrap';
import { ErrorPopover } from 'components/lookup-tables';
import { LookupTablesActions } from 'stores/lookup-tables/LookupTablesStore';
import useGetPermissionsByScope from 'logic/lookup-tables/useScopePermissions';
import type { LookupTable, LookupTableCache, LookupTableAdapter } from 'logic/lookup-tables/types';

type Props = {
  table: LookupTable,
  cache: LookupTableCache,
  dataAdapter: LookupTableAdapter,
  errors?: {
    table: LookupTable,
    cache: LookupTableCache,
    dataAdapter: LookupTableAdapter,
  },
};

const Actions = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
`;

const LUTTableEntry = ({ table, cache, dataAdapter, errors }: Props) => {
  const history = useHistory();
  const { getPermissionsByScope } = useGetPermissionsByScope();

  const showAction = (inTable: LookupTable, action: string): boolean => {
    // TODO: Update this method to check for the metadata
    const permissions = getPermissionsByScope(inTable._metadata?.scope);

    return permissions[action];
  };

  const handleDelete = (inTable: LookupTable) => (_event: React.SyntheticEvent) => {
    // eslint-disable-next-line no-alert
    const shouldDelete = window.confirm(
      `Are you sure you want to delete lookup table "${inTable.title}"?`,
    );

    if (shouldDelete) {
      LookupTablesActions.delete(inTable.id).then(() => LookupTablesActions.reloadPage());
    }
  };

  const handleEdit = (tableName: string) => (_event: React.SyntheticEvent) => {
    history.push(Routes.SYSTEM.LOOKUPTABLES.edit(tableName));
  };

  return (
    <tbody>
      <tr>
        <td>
          {errors.table && (
            <ErrorPopover placement="right" errorText={errors.table} title="Lookup Table problem" />
          )}
          <Link to={Routes.SYSTEM.LOOKUPTABLES.show(table.name)}>{table.title}</Link>
        </td>
        <td>{table.description}</td>
        <td>{table.name}</td>
        <td>
          {errors.cache && (
            <ErrorPopover placement="bottom" errorText={errors.cache} title="Cache problem" />
          )}
          <Link to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(cache.name)}>{cache.title}</Link>
        </td>
        <td>
          {errors.dataAdapter && (
            <ErrorPopover placement="bottom" errorText={errors.dataAdapter} title="Data adapter problem" />
          )}
          <Link to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(dataAdapter.name)}>{dataAdapter.title}</Link>
        </td>
        <td>
          <Actions>
            {showAction(table, 'edit') && (
              <Button bsSize="xsmall" bsStyle="info" onClick={handleEdit(table.name)} role="edit-button">
                Edit
              </Button>
            )}
            {showAction(table, 'delete') && (
              <Button style={{ marginLeft: '6px' }}
                      bsSize="xsmall"
                      bsStyle="primary"
                      onClick={handleDelete(table)}
                      role="delete-button">
                Delete
              </Button>
            )}
          </Actions>
        </td>
      </tr>
    </tbody>
  );
};

LUTTableEntry.defaultProps = {
  errors: {
    table: null,
    cache: null,
    dataAdapter: null,
  },
};

export default LUTTableEntry;
