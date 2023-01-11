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
import type { MenuListProps } from 'react-select';
import { components as Components } from 'react-select';
import { VariableSizeList as List } from 'react-window';
import styled from 'styled-components';

import useElementDimensions from 'hooks/useElementDimensions';

const REACT_SELECT_MAX_OPTIONS_LENGTH = 1000;
const MAX_CONTAINER_SIZE = 300;
const Container = styled.div<{ height: number}>`
  flex: 1 1 auto;
  height: ${(props) => props?.height || MAX_CONTAINER_SIZE}px;
`;

type RowProps = {
  data: Array<React.ReactNode>,
  index: number,
  setSize: (index: number, size: number) => void,
  containerWidth: number
  style: object
}

const Row = ({ data, index, setSize, style, containerWidth }: RowProps) => {
  const rowRef = useRef(null);

  useEffect(() => {
    setSize(index, rowRef.current.getBoundingClientRect().height);
  }, [setSize, index, containerWidth]);

  return (
    <div ref={rowRef} data-testid="react-window-list-item" style={style}>
      {data[index]}
    </div>
  );
};

export const WindowList = ({ children, listRef, ...rest }: Partial<MenuListProps> & {
  listRef?: React.RefObject<HTMLElement>,
  onItemsRendered?: () => void,
  children: Array<React.ReactNode>,
}) => {
  const containerRef = useRef(null);
  const vListRef = useRef(null);
  const sizeMap = useRef({});
  const containerDimensions = useElementDimensions(containerRef);
  const { width } = containerDimensions;

  const setSize = useCallback((index: number, size: number) => {
    sizeMap.current = { ...sizeMap.current, [index]: size };
    const currentRef = listRef || vListRef;
    currentRef.current?.resetAfterIndex(index);
  }, [listRef]);

  const totalHeight = Object.entries(children).reduce((sum, [index]) => {
    if (sizeMap.current[index] && sum < MAX_CONTAINER_SIZE) {
      return parseInt(sizeMap.current[index], 10) + sum;
    }

    return sum;
  }, 0);

  const getSize = useCallback((index: number) => sizeMap.current[index] || 36, [sizeMap]);

  const itemCount = React.Children.count(children);

  return (
    <Container ref={containerRef} height={totalHeight} data-testid="infinite-loader-container">
      <List<Array<React.ReactNode>> ref={listRef?.current || vListRef?.current}
                                    height={totalHeight || 300}
                                    itemCount={itemCount}
                                    itemSize={getSize}
                                    itemData={children}
                                    width={width}
                                    {...rest}>
        {({ data, index, style }) => (
          <Row data={data}
               style={style}
               index={index}
               setSize={setSize}
               containerWidth={width} />
        )}
      </List>
    </Container>
  );
};

WindowList.defaultProps = {
  listRef: undefined,
  onItemsRendered: undefined,
};

type Props = {
  children: Array<React.ReactNode>,
  innerProps?: object,
}

const CustomMenuList = ({ children, innerProps, ...rest }: Props) => {
  const childrenCount = React.Children.count(children);

  if (!childrenCount || (childrenCount < REACT_SELECT_MAX_OPTIONS_LENGTH)) {
    return (
      <Components.MenuList {...rest}
                           innerProps={{
                             ...innerProps,
                             // @ts-ignore
                             'data-testid': 'react-select-list',
                           }}>
        {children}
      </Components.MenuList>
    );
  }

  return <WindowList>{children}</WindowList>;
};

CustomMenuList.defaultProps = {
  innerProps: {},
};

export default CustomMenuList;
