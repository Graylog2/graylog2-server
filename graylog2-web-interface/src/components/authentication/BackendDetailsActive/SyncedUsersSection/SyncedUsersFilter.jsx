// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import type { PaginatedUsers } from 'actions/users/UsersActions';
import { SearchForm } from 'components/common';
import UserQueryHelper from 'components/users/UsersQueryHelper';

type Props = {
  onSearch: (query: string) => Promise<?PaginatedUsers>,
  onReset: () => Promise<?PaginatedUsers>,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  margin-bottom: 10px;
`;

const SyncedUsersFilter = ({ onSearch, onReset }: Props) => {
  const _handleSearch = (newQuery, resetLoading) => onSearch(newQuery).then(resetLoading);
  const queryHelper = <UserQueryHelper />;

  return (
    <Container>
      <SearchForm onReset={onReset}
                  onSearch={_handleSearch}
                  queryHelpComponent={queryHelper}
                  topMargin={0}
                  useLoadingState />
    </Container>
  );
};

export default SyncedUsersFilter;
