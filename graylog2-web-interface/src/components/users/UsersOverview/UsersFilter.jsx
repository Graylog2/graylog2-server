// @flow strict
import * as React from 'react';

import { SearchForm } from 'components/common';

import UserQueryHelper from '../UsersQueryHelper';

type Props = {
  onSearch: (query: string) => void,
};

const UsersFilter = ({ onSearch }: Props) => (
  <SearchForm onSearch={onSearch}
              wrapperClass="has-bm"
              onReset={() => onSearch('')}
              queryHelpComponent={<UserQueryHelper />}
              topMargin={0} />
);

export default UsersFilter;
