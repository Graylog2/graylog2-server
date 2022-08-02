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

import { LinkContainer, Link } from 'components/common/router';
import Routes from 'routing/Routes';
import { Button, Tooltip } from 'components/bootstrap';
import { OverlayElement } from 'components/common';
import { ErrorPopover } from 'components/lookup-tables';
import { MetricContainer, CounterRate } from 'components/metrics';
import { LookupTableDataAdaptersActions } from 'stores/lookup-tables/LookupTableDataAdaptersStore';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';
import useScopePermissions from 'hooks/useScopePermissions';

type Props = {
  adapter: LookupTableAdapter,
  error: string,
};

const DataAdapterTableEntry = ({ adapter, error = null }: Props) => {
  const { name: adapterName, title: adapterTitle, description: adapterDescription, id: adapterId } = adapter;
  const { getScopePermissions } = useScopePermissions();

  const _onDelete = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to delete data adapter "${adapterTitle}"?`)) {
      LookupTableDataAdaptersActions.delete(adapter.id).then(() => LookupTableDataAdaptersActions.reloadPage());
    }
  };

  const showActions = () => {
    const permissions = getScopePermissions(adapter);

    return permissions.is_mutable;
  };

  const isMutable = showActions();
  const immutableTooltip = <Tooltip id={`${adapterId}-immutable-tooltip`}>Action not available for immutable entities</Tooltip>;

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
          <OverlayElement overlay={immutableTooltip} placement="top" useOverlay={isMutable}>
            <LinkContainer data-testid="edit-button" disabled={!isMutable} to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(adapterName)}>
              <Button bsSize="xsmall" bsStyle="info">Edit</Button>
            </LinkContainer>
          </OverlayElement>
          &nbsp;
          {showActions() ? (
            <Button bsSize="xsmall"
                    bsStyle="primary"
                    onClick={_onDelete}
                    data-testid="delete-button">
              Delete
            </Button>
          ) : (
            <OverlayElement placement="top" overlay={immutableTooltip} useOverlay={isMutable}>
              <Button disabled={!isMutable}
                      bsSize="xsmall"
                      bsStyle="primary"
                      onClick={_onDelete}
                      data-testid="delete-button">
                Delete
              </Button>
            </OverlayElement>
          )}
        </td>
      </tr>
    </tbody>
  );
};

export default DataAdapterTableEntry;
