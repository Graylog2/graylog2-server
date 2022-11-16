import * as React from 'react';
import { useMemo } from 'react';
import styled, { css } from 'styled-components';

import type { Attribute } from './types';

const Td = styled.td<{ $width: string, $maxWidth: string }>(({ $width }) => css`
  width: ${$width ?? 'auto'};
  max-width: ${$width ?? 'none'};
`);

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
    <Td $width={cellRenderer?.width} $maxWidth={cellRenderer?.maxWidth}>
      {content}
    </Td>
  );
};

export default TableCell;
