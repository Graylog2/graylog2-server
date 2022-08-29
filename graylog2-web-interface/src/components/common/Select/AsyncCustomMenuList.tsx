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
import type { Props } from 'react-select';
import InfiniteLoader from 'react-window-infinite-loader';
import styled from 'styled-components';

import { WindowList } from 'components/common/Select/CustomMenuList';

const StyledDiv = styled.div`
  display: flex;
  align-items: center; 
  justify-content: center; 
  height: 36px;
  width: 100%;
`;

const getNoOptionMessgage = () => {
  return (
    <StyledDiv key="noOptions" className="menu-notice menu-notice--no-options">
      No Options
    </StyledDiv>
  );
};

const AsyncCustomMenuList = ({ children, selectProps: { loadOptions, total } }: Props.MenuList) => {
  const items = children?.length ? children : [getNoOptionMessgage()];

  return (
    <InfiniteLoader isItemLoaded={(index: number) => index < children.length}
                    itemCount={total}
                    threshold={30}
                    minimumBatchSize={50}
                    loadMoreItems={loadOptions}>
      {({ onItemsRendered, ref }) => (
        <WindowList listRef={ref}
                    onItemsRendered={onItemsRendered}>
          {items}
        </WindowList>
      )}
    </InfiniteLoader>

  );
};

export default AsyncCustomMenuList;
