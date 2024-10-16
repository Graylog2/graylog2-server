import React from 'react';

import { DataTable, ExternalLink, Spinner } from 'components/common';
import { Alert } from 'components/bootstrap';

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
        <ExternalLink href={plugin.url} style={{ marginLeft: 10 }}>Website</ExternalLink>
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
