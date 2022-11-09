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

type Props<ListItem extends { id: string }> = {
  rows: Array<ListItem>,
  rowActionsRenderer?: (listItem: ListItem) => React.ReactNode,
  cellRenderer?: (listItem: ListItem, attribute: Attribute, defaulConfigurableDataTabletRenderer: (listItem: ListItem, attribute: Attribute) => React.ReactNode) => React.ReactNode,
  headerRenderer?: (attribute: Attribute, defaultRenderer: (attribute: Attribute) => React.ReactNode) => React.ReactNode,
  attributes: Array<string>,
  availableAttributes: Array<Attribute>,
}

// eslint-disable-next-line react/jsx-no-useless-fragment
const defaultHeaderRenderer = (attribute: Attribute) => <th key={attribute.title}>{attribute.title}</th>;
// eslint-disable-next-line react/jsx-no-useless-fragment
const defaultCellRenderer = <ListItem extends { id: string }>(listItem: ListItem, attribute: Attribute) => <td key={`${listItem.id}-${attribute.id}`}>{listItem[attribute.id]}</td>;

const ConfigurableDataTable = <ListItem extends { id: string }>({
  rows,
  headerRenderer,
  cellRenderer,
  attributes,
  availableAttributes,
  rowActionsRenderer,
}: Props<ListItem>) => {
  const selectedAttributes = attributes.map((attributeId) => availableAttributes.find(({ id }) => id === attributeId));

  return (
    <Table striped condensed hover>
      <thead>
        <tr>
          {selectedAttributes.map((attribute) => (
            typeof headerRenderer === 'function'
              ? headerRenderer(attribute, defaultHeaderRenderer)
              : defaultHeaderRenderer(attribute)))}
          {typeof rowActionsRenderer === 'function' ? <th className="text-right">Actions</th> : null}
        </tr>
      </thead>
      <tbody>
        {rows.map((listItem) => (
          <tr key={listItem.id}>
            {selectedAttributes.map((attribute) => (
              typeof cellRenderer === 'function'
                ? cellRenderer(listItem, attribute, defaultCellRenderer)
                : defaultCellRenderer(listItem, attribute)
            ))}
            {typeof rowActionsRenderer === 'function' ? <td className="text-right">{rowActionsRenderer(listItem)}</td> : null}
          </tr>
        ))}
      </tbody>
    </Table>
  );
};

ConfigurableDataTable.defaultProps = {
  cellRenderer: undefined,
  headerRenderer: undefined,
  rowActionsRenderer: undefined,
};

export default ConfigurableDataTable;
