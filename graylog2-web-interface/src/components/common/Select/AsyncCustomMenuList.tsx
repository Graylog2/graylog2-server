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

import { WindowList } from 'components/common/Select/CustomMenuList';

const AsyncCustomMenuList = ({ children, selectProps: { loadOptions, total } }: Props.MenuList) => {
  const childrenArray = React.Children.toArray(children);

  return (

    <InfiniteLoader isItemLoaded={(index) => index < childrenArray.length}
                    itemCount={total}
                    loadMoreItems={loadOptions}>
      {({ onItemsRendered, ref }) => (
        <WindowList listRef={ref} onItemsRendered={onItemsRendered}>{children}
        </WindowList>
      )}
    </InfiniteLoader>

  );
};

export default AsyncCustomMenuList;
