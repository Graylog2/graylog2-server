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
import { PluginStore } from 'graylog-web-plugin/plugin';
import type { PluginExports } from 'graylog-web-plugin/plugin';

import { LinkContainer } from 'components/common/router';
import { Row, Col, Button, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import Routes from 'routing/Routes';
import { LookupTableDataAdaptersActions } from 'stores/lookup-tables/LookupTableDataAdaptersStore';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';
import useScopePermissions from 'hooks/useScopePermissions';

import ConfigSummaryDefinitionListWrapper from './ConfigSummaryDefinitionListWrapper';

type Props = {
  dataAdapter: LookupTableAdapter,
};

const DataAdapter = ({ dataAdapter }: Props) => {
  const [lookupKey, setLookupKey] = React.useState(null);
  const [lookupResult, setLookupResult] = React.useState(null);
  const { getScopePermissions } = useScopePermissions();

  const _onChange = (event) => {
    setLookupKey(getValueFromInput(event.target));
  };

  const _lookupKey = (e) => {
    e.preventDefault();

    LookupTableDataAdaptersActions.lookup(dataAdapter.name, lookupKey).then((result) => {
      setLookupResult(result);
    });
  };

  const showAction = (): boolean => {
    const permissions = getScopePermissions(dataAdapter._scope);

    return permissions.is_mutable;
  };

  const plugins = {};

  const pluginList = PluginStore.exports('lookupTableAdapters' as keyof PluginExports) as Array<any>;

  pluginList.forEach((p) => {
    plugins[p.type] = p;
  });

  const plugin = plugins[dataAdapter.config.type];

  const { title: adapterTitle, description: adapterDescription, name: adapterName } = dataAdapter;

  if (!plugin) {
    return <p>Unknown data adapter type {dataAdapter.config.type}. Is the plugin missing?</p>;
  }

  const summary = plugin.summaryComponent;
  const isEditable = showAction();

  return (
    <Row className="content">
      <Col md={6}>
        <h2>
          {adapterTitle}
          {' '}
          <small>({plugin.displayName})</small>
        </h2>
        <ConfigSummaryDefinitionListWrapper>
          <dl>
            <dt>Description</dt>
            <dd>{adapterDescription || <em>No description.</em>}</dd>
          </dl>
        </ConfigSummaryDefinitionListWrapper>
        <h4>Configuration</h4>
        <ConfigSummaryDefinitionListWrapper>
          {React.createElement(summary, { dataAdapter: dataAdapter })}
        </ConfigSummaryDefinitionListWrapper>
        <LinkContainer disabled={!isEditable} to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(adapterName)}>
          <Button bsStyle="success">Edit</Button>
        </LinkContainer>
      </Col>
      <Col md={6}>
        <h3>Test lookup</h3>
        <p>You can manually trigger the data adapter using this form. The data will be not cached.</p>
        <form onSubmit={_lookupKey}>
          <fieldset>
            <Input type="text"
                   id="key"
                   name="key"
                   label="Key"
                   required
                   onChange={_onChange}
                   help="Key to look up a value for."
                   value={lookupKey} />
            <Button type="submit" bsStyle="success">Look up</Button>
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
