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
import { Row, Col, Button, Input, Label } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import { LookupTableDataAdaptersActions } from 'stores/lookup-tables/LookupTableDataAdaptersStore';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';
import useScopePermissions from 'hooks/useScopePermissions';
import { useModalContext } from 'components/lookup-tables/LUTModals/ModalContext';

import type { DataAdapterPluginType } from './types';
import ConfigSummaryDefinitionListWrapper from './ConfigSummaryDefinitionListWrapper';

type Props = {
  dataAdapter: LookupTableAdapter;
};

const DataAdapter = ({ dataAdapter }: Props) => {
  const [lookupKey, setLookupKey] = React.useState('');
  const [lookupResult, setLookupResult] = React.useState(null);
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(dataAdapter);
  const { setModal, setTitle, setEntity } = useModalContext();

  const _onChange = (event: React.SyntheticEvent) => {
    setLookupKey(getValueFromInput(event.target));
  };

  const _lookupKey = (event: React.SyntheticEvent) => {
    event.preventDefault();

    LookupTableDataAdaptersActions.lookup(dataAdapter.name, lookupKey).then((result: LookupTableAdapter[]) => {
      setLookupResult(result);
    });
  };

  const handleEdit = () => {
    setModal('DATA-ADAPTER-EDIT');
    setTitle(dataAdapter.name);
    setEntity(dataAdapter);
  }

  const plugin = usePluginEntities('lookupTableAdapters').find(
    (p: DataAdapterPluginType) => p.type === dataAdapter.config?.type,
  );

  if (!plugin) {
    return <p>Unknown data adapter type {dataAdapter.config.type}. Is the plugin missing?</p>;
  }

  const { description: adapterDescription } = dataAdapter;
  const summary = plugin.summaryComponent;

  return (
    <Row className="content">
      <Col md={12}>
        <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
          <Label>{plugin.displayName}</Label>
          {!loadingScopePermissions && scopePermissions?.is_mutable && (
            <Button bsStyle="primary" onClick={handleEdit} role="button" name="edit_square">
              Edit
            </Button>
          )}
        </div>
        <ConfigSummaryDefinitionListWrapper>
          <dl>
            <dt>Description</dt>
            <dd>{adapterDescription || <em>No description.</em>}</dd>
          </dl>
        </ConfigSummaryDefinitionListWrapper>
        <hr />
        <h4>Configuration</h4>
        <ConfigSummaryDefinitionListWrapper>
          {React.createElement(summary, { dataAdapter: dataAdapter })}
        </ConfigSummaryDefinitionListWrapper>
        <hr />
        <h3>Test lookup</h3>
        <p>You can manually trigger the data adapter using this form. The data will be not cached.</p>
        <form onSubmit={_lookupKey}>
          <fieldset>
            <Input
              type="text"
              id="key"
              name="key"
              label="Key"
              required
              onChange={_onChange}
              help="Key to look up a value for."
              value={lookupKey}
            />
            <Button type="submit" bsStyle="success">
              Look up
            </Button>
          </fieldset>
        </form>
        {lookupResult && (
          <div>
            <h4>Lookup result</h4>
            <pre>{JSON.stringify(lookupResult, null, 2)}</pre>
          </div>
        )}
      </Col>
    </Row>
  );
};

export default DataAdapter;
