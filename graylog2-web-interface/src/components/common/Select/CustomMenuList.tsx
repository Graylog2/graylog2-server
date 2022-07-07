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
import React, { useRef, useCallback, useEffect } from 'react';
import { components as Components } from 'react-select';
import type { Props } from 'react-select/creatable';
import { VariableSizeList as List } from 'react-window';
import AutoSizer from 'react-virtualized-auto-sizer';
import styled from 'styled-components';

import useWindowResize from 'hooks/useWindowResize';

const Container = styled.div`
  flex: 1 1 auto;
  height: 100vh;
  max-height: 300px;
`;
const REACT_SELECT_MAX_OPTIONS_LENGTH = 1000;

type RowProps = {
  data: Array<object>,
  index: number,
  setSize: (index: number, size: number) => void,
  windowWidth: number
}

const Row = ({ data, index, setSize, windowWidth }: RowProps) => {
  const rowRef = useRef(null);

  useEffect(() => {
    setSize(index, rowRef.current.getBoundingClientRect().height);
  }, [setSize, index, windowWidth]);

  return (
    <div ref={rowRef} data-testid="react-window-list-item">
      {data[index]}
    </div>
  );
};

const WindowList = ({ rows }: Props.MenuList) => {
  const listRef = useRef(null);
  const sizeMap = useRef({});

  const setSize = useCallback((index, size) => {
    sizeMap.current = { ...sizeMap.current, [index]: size };
    listRef.current.resetAfterIndex(index);
  }, []);

  const getSize = (index) => sizeMap.current[index] || 36;

  const [windowWidth] = useWindowResize();

  return (
    <Container>
      <AutoSizer>
        {
          ({ width, height }) => (
            <List ref={listRef}
                  height={height}
                  itemCount={rows.length}
                  itemSize={getSize}
                  itemData={rows}
                  width={width}>
              {({ data, index, style }) => (
                <div style={style}>
                  <Row data={data}
                       index={index}
                       setSize={setSize}
                       windowWidth={windowWidth} />
                </div>
              )}
            </List>
          )
        }
      </AutoSizer>
    </Container>
  );
};

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

  return <WindowList rows={rows}>{children}</WindowList>;
};

CustomMenuList.defaultProps = {
  innerProps: {},
};

export default CustomMenuList;
