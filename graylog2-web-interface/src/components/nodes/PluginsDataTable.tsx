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
import React from 'react';
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import { DataTable, ExternalLink, Spinner } from 'components/common';
import { Alert } from 'components/bootstrap';

const StyledExternalLink = styled(ExternalLink)`
  margin-left: 10px;
`;

const headerCellFormatter = (header) => <th>{header}</th>;

type Props = {
  plugins?: any[];
};

const PluginsDataTable = ({ plugins = undefined }: Props) => {
  const { t } = useTranslation('system');

  if (!plugins) {
    return <Spinner text={t('plugins_data_table.loading')} />;
  }

  if (plugins.length === 0) {
    return <Alert bsStyle="info">{t('plugins_data_table.no_plugins')}</Alert>;
  }

  const headers = [
    t('plugins_data_table.name'),
    t('plugins_data_table.version'),
    t('plugins_data_table.author'),
    t('plugins_data_table.description'),
  ];

  const pluginInfoFormatter = (plugin) => (
    <tr key={plugin.name}>
      <td className="limited">{plugin.name}</td>
      <td className="limited">{plugin.version}</td>
      <td className="limited">{plugin.author}</td>
      <td className="limited" style={{ width: '50%' }}>
        {plugin.description}
        &nbsp;&nbsp;
        <StyledExternalLink href={plugin.url}>{t('plugins_data_table.website')}</StyledExternalLink>
      </td>
    </tr>
  );

  return (
    <DataTable
      id="plugin-list"
      rowClassName="row-sm"
      condensed
      headers={headers}
      headerCellFormatter={headerCellFormatter}
      sortByKey="name"
      rows={plugins}
      dataRowFormatter={pluginInfoFormatter}
      filterLabel={t('plugins_data_table.filter')}
      filterKeys={[]}
    />
  );
};

export default PluginsDataTable;
