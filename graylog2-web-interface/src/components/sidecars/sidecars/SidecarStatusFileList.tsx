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

import { DataTable, Timestamp, Icon } from 'components/common';

const _headerCellFormatter = (header) => <th>{header}</th>;

const _activityFormatter = (time) => {
  const now = new Date().getTime();
  const modDate = new Date(time).getTime();

  if (modDate > now - 60000) {
    return 'info';
  }

  return '';
};

const _dirFormatter = (file) => {
  if (file.is_dir) {
    return (
      <span>
        <Icon name="folder_open" />
        &nbsp;&nbsp;{file.path}
      </span>
    );
  }

  return (
    <span>
      <Icon name="notes" type="regular" />
      &nbsp;&nbsp;{file.path}
    </span>
  );
};

const _fileListFormatter = (file) => (
  <tr key={file.path} className={_activityFormatter(file.mod_time)}>
    <td className="limited">
      <Timestamp dateTime={file.mod_time} />
    </td>
    <td className="limited">{file.size}</td>
    <td>{_dirFormatter(file)}</td>
  </tr>
);

type Props = {
  files: any[];
};

const SidecarStatusFileList = ({ files }: Props) => {
  const filterKeys = [];
  const headers = ['Modified', 'Size', 'Path'];

  return (
    <div>
      <DataTable
        id="log-file-list"
        className="table-hover"
        headers={headers}
        headerCellFormatter={_headerCellFormatter}
        rows={files}
        dataRowFormatter={_fileListFormatter}
        filterLabel="Filter Files"
        filterKeys={filterKeys}
      />
    </div>
  );
};

export default SidecarStatusFileList;
