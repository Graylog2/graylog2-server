// @flow strict
import * as React from 'react';

import { SearchForm } from 'components/common';
import type { PaginatedUsers } from 'actions/users/UsersActions';

import UserQueryHelper from '../UsersQueryHelper';

type Props = {
  onSearch: (query: string) => Promise<?PaginatedUsers>,
  onReset: () => Promise<?PaginatedUsers>,
};

const UsersFilter = ({ onSearch, onReset }: Props) => {
  const _handleSearch = (newQuery, resetLoading) => onSearch(newQuery).then(resetLoading);
  const queryHelper = <UserQueryHelper />;

  return (
    <SearchForm onSearch={_handleSearch}
                wrapperClass="has-bm"
                onReset={onReset}
                useLoadingState
                queryHelpComponent={queryHelper}
                topMargin={0} />

  );
};

export default UsersFilter;
