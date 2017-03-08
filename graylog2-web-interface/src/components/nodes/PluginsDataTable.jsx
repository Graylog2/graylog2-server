import React, { PropTypes } from 'react';
import { Alert } from 'react-bootstrap';

import { DataTable, Spinner } from 'components/common';

const PluginsDataTable = React.createClass({
  propTypes: {
    plugins: PropTypes.array,
  },
  _headerCellFormatter(header) {
    return <th>{header}</th>;
  },
  _pluginInfoFormatter(plugin) {
    return (
      <tr key={plugin.name}>
        <td className="limited">{plugin.name}</td>
        <td className="limited">{plugin.version}</td>
        <td className="limited">{plugin.author}</td>
        <td className="limited" style={{ width: '50%' }}>
          {plugin.description}
          &nbsp;
          <a href={plugin.url} target="_blank" style={{ marginLeft: 10 }}><i className="fa fa-external-link" /> Website</a>
        </td>
      </tr>
    );
  },
  render() {
    if (!this.props.plugins) {
      return <Spinner text="Loading plugins on this node..." />;
    }

    if (this.props.plugins.length === 0) {
      return <Alert bsStyle="info"><i className="fa fa-info-circle" />&nbsp; This node has not any installed plugins.</Alert>;
    }

    const headers = ['Name', 'Version', 'Author', 'Description'];

    return (
      <DataTable id="plugin-list"
                 rowClassName="row-sm"
                 className="table-hover table-condensed table-striped"
                 headers={headers}
                 headerCellFormatter={this._headerCellFormatter}
                 sortByKey={'name'}
                 rows={this.props.plugins}
                 dataRowFormatter={this._pluginInfoFormatter}
                 filterLabel="Filter"
                 filterKeys={[]} />
    );
  },
});

export default PluginsDataTable;
