import PropTypes from 'prop-types';
import React from 'react';

import { DataTable, Spinner, Timestamp } from 'components/common';

const SidecarStatusFileList = React.createClass({
  propTypes: {
    files: PropTypes.array.isRequired,
  },

  _headerCellFormatter(header) {
    return <th>{header}</th>;
  },

  _activityFormatter(time) {
    var now = new Date().getTime();
    var modDate = new Date(time).getTime();
    if (modDate > (now - 60000)) {
      return("info");
    } else {
      return("");
    }
  },

  _dirFormatter(file) {
    if(file.is_dir) {
      return(<span><i className="fa fa-folder-open"/>&nbsp;&nbsp;{file.path}</span>);
    } else {
      return(<span><i className="fa fa-file-o"/>&nbsp;&nbsp;{file.path}</span>);
    }
  },

  _fileListFormatter(file) {
    const format = "YYYY-MM-DD HH:mm:ss";

    return (
      <tr key={file.path} className={this._activityFormatter(file.mod_time)}>
        <td className="limited"><Timestamp dateTime={file.mod_time} format={format}/></td>
        <td className="limited">{file.size}</td>
        <td>{this._dirFormatter(file)}</td>
      </tr>
    );
  },

  render() {
    var filterKeys = [];
    var headers = ["Modified", "Size", "Path"];

    return (
      <div>
        <DataTable id="log-file-list"
                   className="table-hover"
                   headers={headers}
                   headerCellFormatter={this._headerCellFormatter}
                   rows={this.props.files}
                   dataRowFormatter={this._fileListFormatter}
                   filterLabel="Filter Files"
                   filterKeys={filterKeys}>
        </DataTable>
      </div>
    );
  }
});

export default SidecarStatusFileList;
