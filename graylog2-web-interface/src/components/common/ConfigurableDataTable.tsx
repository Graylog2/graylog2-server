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
import styled from 'styled-components';

import { Table } from 'components/bootstrap';
import { TextOverflowEllipsis } from 'components/common/index';

type Attribute = {
  id: string,
  title: string,
  type?: boolean,
};

type CustomHeaders = { [key: string]: (attribute: Attribute) => React.ReactNode }
export type CustomCells<ListItem extends { id: string}> = { [key: string]: (listItem: ListItem, attribute: Attribute, key: string) => React.ReactNode }

const ScrollContainer = styled.div`
  overflow-x: auto;
`;

const ActionsHead = styled.th`
  text-align: right;
`;

const ActionsCell = styled.td`
  > div {
    display: flex;
    justify-content: right;
  }
`;

const DescriptionCell = styled.td`
  max-width: 30vw;
`;

const attributeCellRenderer = {
  description: (listItem, _attribute, key) => (
    <DescriptionCell key={key}>
      <TextOverflowEllipsis>
        {listItem.description}
      </TextOverflowEllipsis>
    </DescriptionCell>
  ),
};

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
      {displayActionsCol ? <ActionsHead>Actions</ActionsHead> : null}
    </tr>
  </thead>
);

type Props<ListItem extends { id: string }> = {
  rows: Array<ListItem>,
  rowActions?: (listItem: ListItem) => React.ReactNode,
  customCells?: CustomCells<ListItem>,
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
    <ScrollContainer>
      <Table striped condensed hover>
        <TableHead selectedAttributes={selectedAttributes}
                   customHeaders={customHeaders}
                   displayActionsCol={displayActionsCol} />
        <tbody>
          {rows.map((listItem) => (
            <tr key={listItem.id}>
              {selectedAttributes.map((attribute) => {
                const cellKey = `${listItem.id}-${attribute.id}`;
                const cellRenderer = customCells?.[attribute.id] ?? attributeCellRenderer[attribute.id];

                return (
                  cellRenderer
                    ? cellRenderer(listItem, attribute, cellKey)
                    : <td key={cellKey}>{listItem[attribute.id]}</td>
                );
              })}
              {displayActionsCol ? <ActionsCell>{rowActions(listItem)}</ActionsCell> : null}
            </tr>
          ))}
        </tbody>
      </Table>
    </ScrollContainer>
  );
};

ConfigurableDataTable.defaultProps = {
  customCells: undefined,
  customHeaders: undefined,
  rowActions: undefined,
};

export default ConfigurableDataTable;
