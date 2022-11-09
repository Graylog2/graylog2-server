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

import { Table } from 'components/bootstrap';

type Attribute = {
  id: string,
  title: string,
  type?: boolean,
};

type CustomHeaders = { [key: string]: (attribute: Attribute) => React.ReactNode }

const TableHead = ({
  selectedAttributes,
  customHeaders,
  displayActionsCol,
}: {
  selectedAttributes: Array<Attribute>,
  customHeaders: CustomHeaders,
  displayActionsCol: boolean
}) => (
  <thead>
    <tr>
      {selectedAttributes.map((attribute) => {
        const headerKey = attribute.title;

        return (
          customHeaders?.[attribute.id]
            ? customHeaders[attribute.id](attribute)
            : <th key={headerKey}>{attribute.title}</th>
        );
      })}
      {displayActionsCol ? <th className="text-right">Actions</th> : null}
    </tr>
  </thead>
);

type Props<ListItem extends { id: string }> = {
  rows: Array<ListItem>,
  rowActions?: (listItem: ListItem) => React.ReactNode,
  customCells?: { [key: string]: (listItem: ListItem, attribute: Attribute, key: string) => React.ReactNode }
  customHeaders?: CustomHeaders,
  attributes: Array<string>,
  availableAttributes: Array<Attribute>,
};

const ConfigurableDataTable = <ListItem extends { id: string }>({
  rows,
  customHeaders,
  customCells,
  attributes,
  availableAttributes,
  rowActions,
}: Props<ListItem>) => {
  const selectedAttributes = attributes.map((attributeId) => availableAttributes.find(({ id }) => id === attributeId));
  const displayActionsCol = typeof rowActions === 'function';

  return (
    <Table striped condensed hover>
      <TableHead selectedAttributes={selectedAttributes}
                 customHeaders={customHeaders}
                 displayActionsCol={displayActionsCol} />
      <tbody>
        {rows.map((listItem) => (
          <tr key={listItem.id}>
            {selectedAttributes.map((attribute) => {
              const cellKey = `${listItem.id}-${attribute.id}`;

              return (
                customCells?.[attribute.id]
                  ? customCells[attribute.id](listItem, attribute, cellKey)
                  : <td key={cellKey}>{listItem[attribute.id]}</td>
              );
            })}
            {displayActionsCol ? <td className="text-right">{rowActions(listItem)}</td> : null}
          </tr>
        ))}
      </tbody>
    </Table>
  );
};

ConfigurableDataTable.defaultProps = {
  customCells: undefined,
  customHeaders: undefined,
  rowActions: undefined,
};

export default ConfigurableDataTable;
