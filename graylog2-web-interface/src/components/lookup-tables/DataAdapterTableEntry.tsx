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
import { Spinner } from 'components/common';
import Routes from 'routing/Routes';
import { Button, ButtonToolbar } from 'components/bootstrap';
import { ErrorPopover } from 'components/lookup-tables';
import { MetricContainer, CounterRate } from 'components/metrics';
import { LookupTableDataAdaptersActions } from 'stores/lookup-tables/LookupTableDataAdaptersStore';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';
import useScopePermissions from 'hooks/useScopePermissions';

const Actions = styled(ButtonToolbar)`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
`;

type Props = {
  adapter: LookupTableAdapter,
  error: string,
  onDelete?: () => void
};

const DataAdapterTableEntry = ({ adapter, error = null, onDelete }: Props) => {
  const history = useHistory();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(adapter);
  const { name: adapterName, title: adapterTitle, description: adapterDescription, id: adapterId } = adapter;

  const handleEdit = () => {
    history.push(Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(adapterName));
  };

  const handleDelete = () => {
    // eslint-disable-next-line no-alert
    const shouldDelete = window.confirm(`Are you sure you want to delete data adapter "${adapterTitle}"?`);

    if (shouldDelete) {
      LookupTableDataAdaptersActions.delete(adapter.id).then(() => onDelete());
    }
  };

  return (
    <tbody>
      <tr>
        <td>
          {error && <ErrorPopover errorText={error} title="Lookup table problem" placement="right" />}
          <Link to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(adapterName)}>{adapterTitle}</Link>
        </td>
        <td>{adapterDescription}</td>
        <td>{adapterName}</td>
        <td>
          <MetricContainer name={`org.graylog2.lookup.adapters.${adapterId}.requests`}>
            <CounterRate suffix="lookups/s" />
          </MetricContainer>
        </td>
        <td>
          {loadingScopePermissions ? <Spinner /> : scopePermissions.is_mutable && (
            <Actions>
              <Button bsSize="xsmall"
                      bsStyle="info"
                      onClick={handleEdit}
                      role="button"
                      name="edit">
                Edit
              </Button>
              <Button style={{ marginLeft: '6px' }}
                      bsSize="xsmall"
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

DataAdapterTableEntry.defaultProps = {
  onDelete: () => {},
};

export default DataAdapterTableEntry;
