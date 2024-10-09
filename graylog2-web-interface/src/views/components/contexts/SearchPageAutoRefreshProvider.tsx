import * as React from 'react';
import { useCallback } from 'react';

import AutoRefreshProvider from 'views/components/contexts/AutoRefreshProvider';
import { execute } from 'views/logic/slices/searchExecutionSlice';
import useAppDispatch from 'stores/useAppDispatch';
import useAppSelector from 'stores/useAppSelector';
import { selectJobIds } from 'views/logic/slices/searchExecutionSelectors';

const SearchPageAutoRefreshProvider = ({ children }: React.PropsWithChildren) => {
  const dispatch = useAppDispatch();
  const jobIds = useAppSelector(selectJobIds);

  const onRefresh = useCallback(() => {
    if (!jobIds) {
      dispatch(execute());
    }
  }, [dispatch, jobIds]);

  return (
    <AutoRefreshProvider onRefresh={onRefresh}>
      {children}
    </AutoRefreshProvider>
  );
};

export default SearchPageAutoRefreshProvider;
