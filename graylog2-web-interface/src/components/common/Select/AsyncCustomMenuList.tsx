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
