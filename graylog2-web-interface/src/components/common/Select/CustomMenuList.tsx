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
import { List } from 'react-virtualized';
import { components as Components } from 'react-select';
import type { Props } from 'react-select/creatable';

const REACT_SELECT_MAX_OPTIONS_LENGTH = 1000;

const CustomMenuList = ({ children, innerProps, ...rest }: Props.MenuList) => {
  const rows = React.Children.toArray(children);

  if (rows.length < REACT_SELECT_MAX_OPTIONS_LENGTH) {
    return (
      <Components.MenuList {...rest}
                           innerProps={{
                             ...innerProps,
                             'data-testid': 'react-select-list',
                           }}>
        {children}
      </Components.MenuList>
    );
  }

  const rowRenderer = ({ key, index, style }) => {
    return <div data-testid="react-virtualized-list-item" key={key} style={style}>{rows[index]}</div>;
  };

  return (
    <List style={{ width: '100%' }}
          width={1}
          height={300}
          rowHeight={36}
          rowCount={rows.length}
          rowRenderer={rowRenderer}
          containerStyle={{
            width: '100%',
            maxWidth: '100%',
          }} />
  );
};

CustomMenuList.defaultProps = {
  innerProps: {},
};

export default CustomMenuList;
