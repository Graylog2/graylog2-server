// @flow strict
import * as React from 'react';

import { SearchForm } from 'components/common';

import UserQueryHelper from '../UsersQueryHelper';

type Props = {
  onSearch: (query: string) => void,
  onReset: () => void,
};

const UsersFilter = ({ onSearch, onReset }: Props) => (
  <SearchForm onSearch={onSearch}
              wrapperClass="has-bm"
              onReset={onReset}
              queryHelpComponent={<UserQueryHelper />}
              topMargin={0} />

);

export default UsersFilter;
