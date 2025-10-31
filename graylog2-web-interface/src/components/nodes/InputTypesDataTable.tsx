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

import { Alert } from 'components/bootstrap';
import { DataTable, ExternalLink, Spinner } from 'components/common';

const headerCellFormatter = (header: React.ReactNode) => <th>{header}</th>;
const inputTypeFormatter = (inputType: { name: string; type: string; link_to_docs?: string }) => (
  <tr key={inputType.type}>
    <td className="limited">{inputType.name}</td>
    <td className="limited">{inputType.type}</td>
    <td className="limited" style={{ width: 150 }}>
      {inputType.link_to_docs && <ExternalLink href={inputType.link_to_docs}>Documentation</ExternalLink>}
    </td>
  </tr>
);

type Props = {
  inputDescriptions?: any;
};

const InputTypesDataTable = ({ inputDescriptions = undefined }: Props) => {
  if (!inputDescriptions) {
    return <Spinner text="Loading input types..." />;
  }

  if (Object.keys(inputDescriptions).length === 0) {
    return <Alert bsStyle="warning">Input types are unavailable.</Alert>;
  }

  const headers = ['Name', 'Type', 'Documentation'];

  const rows = Object.keys(inputDescriptions).map((key) => inputDescriptions[key]);

  return (
    <DataTable
      id="input-types-list"
      rowClassName="row-sm"
      className="table-hover table-condensed table-striped"
      headers={headers}
      headerCellFormatter={headerCellFormatter}
      sortByKey="name"
      rows={rows}
      dataRowFormatter={inputTypeFormatter}
      filterLabel="Filter"
      filterKeys={[]}
    />
  );
};

export default InputTypesDataTable;
