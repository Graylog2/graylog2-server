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
import React, { useRef, useCallback, useEffect, useState, useMemo } from 'react';
import { components as Components } from 'react-select';
import type { MenuListProps } from 'react-select';
import { List, type RowComponentProps } from 'react-window';
import styled from 'styled-components';

import useElementDimensions from 'hooks/useElementDimensions';

const REACT_SELECT_MAX_OPTIONS_LENGTH = 1000;
const MAX_CONTAINER_SIZE = 300;
const Container = styled.div<{ height: number }>`
  flex: 1 1 auto;
  height: ${(props) => props?.height || MAX_CONTAINER_SIZE}px;
`;

type RowProps = {
  data: Array<React.ReactNode>;
  setSize: (index: number, size: number) => void;
  containerWidth: number;
};

const Row = ({ data, index, setSize, style, containerWidth }: RowComponentProps<RowProps>) => {
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

type WindowListProps = Partial<MenuListProps> & {
  listRef?: any;
  children: Array<React.ReactNode>;
  onRowsRendered?: (
    visibleRows: { startIndex: number; stopIndex: number },
    allRows: { startIndex: number; stopIndex: number },
  ) => void;
};

export const WindowList = ({ children, listRef = undefined, onRowsRendered = undefined, ...rest }: WindowListProps) => {
  const containerRef = useRef(null);
  const vListRef = useRef(null);
  const [sizeMap, setSizeMap] = useState<Record<number, number>>({});
  const containerDimensions = useElementDimensions(containerRef);
  const { width } = containerDimensions;

  const setSize = useCallback((index: number, size: number) => {
    setSizeMap((prev) => ({ ...prev, [index]: size }));
  }, []);

  const totalHeight = useMemo(() => {
    // Calculate total height based on measured sizes
    // Only sum heights for items that have been measured
    let sum = 0;

    for (let i = 0; i < children.length && sum < MAX_CONTAINER_SIZE; i += 1) {
      const size = sizeMap[i];

      if (size) {
        sum += parseInt(String(size), 10);
      } else {
        // Use default size for unmeasured items
        sum += 36;
      }
    }

    return Math.min(sum, MAX_CONTAINER_SIZE);
  }, [sizeMap, children.length]);

  const getSize = useCallback((index: number) => sizeMap[index] || 36, [sizeMap]);

  return (
    <Container ref={containerRef} height={totalHeight} data-testid="infinite-loader-container">
      <List
        listRef={listRef || vListRef}
        style={{ height: totalHeight || 300, width }}
        rowCount={children.length}
        rowHeight={getSize}
        rowComponent={Row}
        rowProps={{
          data: children,
          setSize,
          containerWidth: width,
        }}
        onRowsRendered={onRowsRendered}
        {...rest}
      />
    </Container>
  );
};

const CustomMenuList = ({
  children,
  innerProps,
  ...rest
}: Partial<MenuListProps> & { children: Array<React.ReactNode> }) => {
  if (!children?.length || children.length < REACT_SELECT_MAX_OPTIONS_LENGTH) {
    return (
      <Components.MenuList
        {...rest}
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

export default CustomMenuList;
