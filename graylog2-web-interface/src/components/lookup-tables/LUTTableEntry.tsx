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

import { Link } from 'components/common/router';
import { Spinner } from 'components/common';
import Routes from 'routing/Routes';
import { Button, ButtonToolbar } from 'components/bootstrap';
import { ErrorPopover } from 'components/lookup-tables';
import { LookupTablesActions } from 'stores/lookup-tables/LookupTablesStore';
import useScopePermissions from 'hooks/useScopePermissions';
import type { LookupTable, LookupTableCache, LookupTableAdapter } from 'logic/lookup-tables/types';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type Props = {
  table: LookupTable,
  cache: LookupTableCache,
  dataAdapter: LookupTableAdapter,
  errors?: {
    table: string,
    cache: string,
    dataAdapter: string,
  },
};

const Actions = styled(ButtonToolbar)`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
`;

const LUTTableEntry = ({
  table, cache, dataAdapter, errors = {
    table: null,
    cache: null,
    dataAdapter: null,
  },
}: Props) => {
  const history = useHistory();
  const sendTelemetry = useSendTelemetry();

  const { loadingScopePermissions, scopePermissions } = useScopePermissions(table);

  const handleDelete = React.useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.LUT.DELETED, {
      app_pathname: 'lut',
      app_section: 'lut',
    });

    // eslint-disable-next-line no-alert
    const shouldDelete = window.confirm(
      `Are you sure you want to delete lookup table "${table.title}"?`,
    );

    if (shouldDelete) {
      LookupTablesActions.delete(table.id).then(() => {
        LookupTablesActions.reloadPage();
      });
    }
  }, [table.id, table.title, sendTelemetry]);

  const handleEdit = React.useCallback(() => {
    history.push(Routes.SYSTEM.LOOKUPTABLES.edit(table.name));
  }, [history, table.name]);

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
          {loadingScopePermissions ? <Spinner /> : scopePermissions.is_mutable && (
            <Actions>
              <Button bsSize="xsmall"
                      onClick={handleEdit}
                      role="button"
                      name="edit_square">
                Edit
              </Button>
              <Button bsSize="xsmall"
                      bsStyle="danger"
                      onClick={handleDelete}
                      role="button"
                      name="delete">
                Delete
              </Button>
            </Actions>
          )}
        </td>
      </tr>
    </tbody>
  );
};

export default LUTTableEntry;
