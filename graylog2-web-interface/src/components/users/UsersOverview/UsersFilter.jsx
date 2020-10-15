// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { SearchForm } from 'components/common';
import type { PaginatedUsers } from 'actions/users/UsersActions';

import UserQueryHelper from '../UsersQueryHelper';

type Props = {
  onSearch: (query: string) => Promise<?PaginatedUsers>,
  onReset: () => Promise<?PaginatedUsers>,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  margin-bottom: 10px;
`;

const UsersFilter = ({ onSearch, onReset }: Props) => {
  const _handleSearch = (newQuery, resetLoading) => onSearch(newQuery).then(resetLoading);
  const queryHelper = <UserQueryHelper />;

  return (
    <Container>
      <SearchForm onSearch={_handleSearch}
                  onReset={onReset}
                  useLoadingState
                  queryHelpComponent={queryHelper}
                  topMargin={0} />
    </Container>
  );
};

export default UsersFilter;
