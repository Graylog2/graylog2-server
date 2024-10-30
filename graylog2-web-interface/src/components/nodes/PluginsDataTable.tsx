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

import { DataTable, ExternalLink, Spinner } from 'components/common';
import { Alert } from 'components/bootstrap';

const StyledExternalLink = styled(ExternalLink)`
  margin-left: 10;
`;

type PluginsDataTableProps = {
  plugins?: any[];
};

class PluginsDataTable extends React.Component<PluginsDataTableProps, {
  [key: string]: any;
}> {
  _headerCellFormatter = (header) => <th>{header}</th>;

  _pluginInfoFormatter = (plugin) => (
    <tr key={plugin.name}>
      <td className="limited">{plugin.name}</td>
      <td className="limited">{plugin.version}</td>
      <td className="limited">{plugin.author}</td>
      <td className="limited" style={{ width: '50%' }}>
        {plugin.description}
          &nbsp;&nbsp;
        <StyledExternalLink href={plugin.url}>Website</StyledExternalLink>
      </td>
    </tr>
  );

  render() {
    if (!this.props.plugins) {
      return <Spinner text="Loading plugins on this node..." />;
    }

    if (this.props.plugins.length === 0) {
      return <Alert bsStyle="info">This node has not any installed plugins.</Alert>;
    }

    const headers = ['Name', 'Version', 'Author', 'Description'];

    return (
      <DataTable id="plugin-list"
                 rowClassName="row-sm"
                 className="table-hover table-condensed table-striped"
                 headers={headers}
                 headerCellFormatter={this._headerCellFormatter}
                 sortByKey="name"
                 rows={this.props.plugins}
                 dataRowFormatter={this._pluginInfoFormatter}
                 filterLabel="Filter"
                 filterKeys={[]} />
    );
  }
}

export default PluginsDataTable;
