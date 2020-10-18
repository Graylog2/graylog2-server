// @flow strict
import * as React from 'react';

import { SearchForm } from 'components/common';

import UserQueryHelper from '../UsersQueryHelper';

type Props = {
  onSearch: (query: string) => void,
  onReset: () => void,
};

const UsersFilter = ({ onSearch, onReset }: Props) => {
  const _handleSearch = (newQuery) => onSearch(newQuery);
  const queryHelper = <UserQueryHelper />;

  return (
    <SearchForm onSearch={_handleSearch}
                wrapperClass="has-bm"
                onReset={onReset}
                queryHelpComponent={queryHelper}
                topMargin={0} />

  );
};

export default UsersFilter;
