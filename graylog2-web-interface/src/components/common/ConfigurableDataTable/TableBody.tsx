import * as React from 'react';
import styled from 'styled-components';
import { useMemo } from 'react';

import { TextOverflowEllipsis } from 'components/common';

import type { CustomCells, Attribute } from './types';

const ActionsCell = styled.td`
  > div {
    display: flex;
    justify-content: right;
  }
`;

const defaultAttributeCellRenderer = {
  description: {
    renderCell: (listItem) => (
      <TextOverflowEllipsis>
        {listItem.description}
      </TextOverflowEllipsis>
    ),
    maxWidth: '30vw',
  },
};

const TableCell = <ListItem extends { id: string }>({
  cellRenderer,
  listItem,
  attribute,
}: {
  cellRenderer: {
    renderCell: (listItem: ListItem, attribute: Attribute) => React.ReactNode,
    width?: string,
    maxWidth?: string,
  },
  listItem: ListItem,
  attribute: Attribute

}) => {
  const content = useMemo(
    () => (cellRenderer ? cellRenderer.renderCell(listItem, attribute) : listItem[attribute.id]),
    [attribute, cellRenderer, listItem],
  );

  return (
    <td style={{ width: cellRenderer?.width, maxWidth: cellRenderer?.maxWidth }}>
      {content}
    </td>
  );
};

type Props<ListItem extends { id: string }> = {
  rows: Array<ListItem>,
  rowActions?: (listItem: ListItem) => React.ReactNode,
  customCells?: CustomCells<ListItem>,
  visibleAttributes: Array<Attribute>,
  displayRowActions: boolean,
};

const TableBody = <ListItem extends { id: string }>({
  rows,
  visibleAttributes,
  displayRowActions,
  customCells,
  rowActions,
}: Props<ListItem>) => (
  <tbody>
    {rows.map((listItem) => (
      <tr key={listItem.id}>
        {visibleAttributes.map((attribute) => {
          const cellRenderer = customCells?.[attribute.id] ?? defaultAttributeCellRenderer[attribute.id];

          return (
            <TableCell cellRenderer={cellRenderer}
                       listItem={listItem}
                       attribute={attribute}
                       key={`${listItem.id}-${attribute.id}`} />
          );
        })}
        {displayRowActions ? <ActionsCell>{rowActions(listItem)}</ActionsCell> : null}
      </tr>
    ))}
  </tbody>
  );

TableBody.defaultProps = {
  customCells: undefined,
  rowActions: undefined,
};

export default TableBody;
