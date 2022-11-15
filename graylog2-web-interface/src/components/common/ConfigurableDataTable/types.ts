import type * as React from 'react';

export type Attribute = {
  id: string,
  title: string,
  type?: boolean,
};

export type CustomHeaders = {
  [key: string]: {
    renderHeader: (attribute: Attribute) => React.ReactNode,
    textAlign?: string,
  }
}

export type CustomCells<ListItem extends { id: string }> = {
  [attributeId: string]: {
    renderCell: (listItem: ListItem, attribute: Attribute) => React.ReactNode,
    textAlign?: string,
    width?: string,
    maxWidth?: string,
  }
}
