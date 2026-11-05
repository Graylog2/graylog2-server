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
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import useHistory from 'routing/useHistory';
import * as JSON from 'util/json';
import Routes from 'routing/Routes';
import usePluginEntities from 'hooks/usePluginEntities';
import { Row, Col, Button, Input, Label } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import { lookupDataAdapter } from 'components/lookup-tables/hooks/api/lookupTablesAPI';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';
import useScopePermissions from 'hooks/useScopePermissions';

import type { DataAdapterPluginType } from './types';
import ConfigSummaryDefinitionListWrapper from './ConfigSummaryDefinitionListWrapper';

type Props = {
  dataAdapter: LookupTableAdapter;
  noEdit?: boolean;
};

const DataAdapter = ({ dataAdapter, noEdit = false }: Props) => {
  const { t } = useTranslation('lookup_tables');
  const [lookupKey, setLookupKey] = useState('');
  const [lookupResult, setLookupResult] = useState(null);
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(dataAdapter);
  const { push } = useHistory();

  const canEdit = !noEdit && !loadingScopePermissions && scopePermissions?.is_mutable;

  const _onChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setLookupKey(String(getValueFromInput(event.target)));
  };

  const _lookupKey = (event: React.SyntheticEvent) => {
    event.preventDefault();

    lookupDataAdapter(dataAdapter.name, lookupKey).then((result) => {
      setLookupResult(result);
    });
  };

  const handleEdit = () => {
    push(Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(dataAdapter.name));
  };

  const plugin = usePluginEntities('lookupTableAdapters').find(
    (p: DataAdapterPluginType) => p.type === dataAdapter.config?.type,
  );

  if (!plugin) {
    return <p>{t('adapter_summary.unknown_type', { type: dataAdapter.config.type })}</p>;
  }

  const { description: adapterDescription } = dataAdapter;
  const summary = plugin.summaryComponent;

  return (
    <Row className="content">
      <Col md={12}>
        <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
          <Label>{plugin.displayName}</Label>
          {canEdit && (
            <Button bsStyle="primary" onClick={handleEdit} role="button" name="edit_square">
              {t('common:actions.edit')}
            </Button>
          )}
        </div>
        <ConfigSummaryDefinitionListWrapper>
          <dl>
            <dt>{t('shared.description')}</dt>
            <dd>{adapterDescription || <em>{t('shared.no_description')}</em>}</dd>
          </dl>
        </ConfigSummaryDefinitionListWrapper>
        <hr />
        <h4>{t('shared.configuration')}</h4>
        <ConfigSummaryDefinitionListWrapper>
          {React.createElement(summary, { dataAdapter: dataAdapter })}
        </ConfigSummaryDefinitionListWrapper>
        <hr />
        <h3>{t('adapter_summary.test_lookup')}</h3>
        <p>{t('adapter_summary.test_lookup_description')}</p>
        <form onSubmit={_lookupKey}>
          <fieldset>
            <Input
              type="text"
              id="key"
              name="key"
              label={t('adapter_summary.key_label')}
              required
              onChange={_onChange}
              help={t('adapter_summary.key_help')}
              value={lookupKey}
            />
            <Button type="submit" bsStyle="primary">
              {t('adapter_summary.look_up')}
            </Button>
          </fieldset>
        </form>
        {lookupResult && (
          <div>
            <h4>{t('adapter_summary.lookup_result')}</h4>
            <pre>{JSON.stringify(lookupResult, null, 2)}</pre>
          </div>
        )}
      </Col>
    </Row>
  );
};

export default DataAdapter;
