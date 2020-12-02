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

    return (<span><Icon name="file" type="regular" />&nbsp;&nbsp;{file.path}</span>);
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
