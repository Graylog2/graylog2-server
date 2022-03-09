import React from 'react';
import { List } from 'react-virtualized';
import { components as Components } from 'react-select';

const REACT_SELECT_MAX_OPTIONS_LENGTH = 1000;

const CustomMenuList = ({ children, ...rest }: { children: React.ReactElement}) => {
  const rows = React.Children.toArray(children);
  if (rows.length < REACT_SELECT_MAX_OPTIONS_LENGTH) return <Components.MenuList {...rest}>{children}</Components.MenuList>;

  const rowRenderer = ({ key, index, style }) => {
    return <div key={key} style={style}>{rows[index]}</div>;
  };

  return (
    <List style={{ width: '100%' }}
          width={300}
          height={300}
          rowHeight={30}
          rowCount={rows.length}
          rowRenderer={rowRenderer} />
  );
};

export default CustomMenuList;
