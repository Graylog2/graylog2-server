import PropTypes from 'prop-types';
import React from 'react';

import { DataTable, Timestamp, Icon } from 'components/common';


class SidecarStatusFileList extends React.Component {
  static propTypes = {
    files: PropTypes.array.isRequired,
  };

  _headerCellFormatter = (header) => {
    return <th>{header}</th>;
  };

  _activityFormatter = (time) => {
    const now = new Date().getTime();
    const modDate = new Date(time).getTime();
    if (modDate > (now - 60000)) {
      return ('info');
    }
    return ('');
  };

  _dirFormatter = (file) => {
    if (file.is_dir) {
      return (<span><Icon name="folder-open" />&nbsp;&nbsp;{file.path}</span>);
    }
    return (<span><Icon name="file-o" />&nbsp;&nbsp;{file.path}</span>);
  };

  _fileListFormatter = (file) => {
    const format = 'YYYY-MM-DD HH:mm:ss';

    return (
      <tr key={file.path} className={this._activityFormatter(file.mod_time)}>
        <td className="limited"><Timestamp dateTime={file.mod_time} format={format} /></td>
        <td className="limited">{file.size}</td>
        <td>{this._dirFormatter(file)}</td>
      </tr>
    );
  };

  render() {
    const filterKeys = [];
    const headers = ['Modified', 'Size', 'Path'];

    return (
      <div>
        <DataTable id="log-file-list"
                   className="table-hover"
                   headers={headers}
                   headerCellFormatter={this._headerCellFormatter}
                   rows={this.props.files}
                   dataRowFormatter={this._fileListFormatter}
                   filterLabel="Filter Files"
                   filterKeys={filterKeys} />
      </div>
    );
  }
}

export default SidecarStatusFileList;
