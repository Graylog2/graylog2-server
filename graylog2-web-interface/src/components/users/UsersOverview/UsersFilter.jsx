// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import UsersActions from 'actions/users/UsersActions';
import { SearchForm } from 'components/common';

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  margin-bottom: 10px;
`;

const UsersFilter = ({ perPage }: { perPage: number }) => {
  const handleSearch = (newQuery, resetLoading) => UsersActions.searchPaginated(1, perPage, newQuery).then(resetLoading);
  const handleReset = () => UsersActions.searchPaginated(1, perPage, '');

  return (
    <Container>
      <SearchForm onSearch={handleSearch}
                  onReset={handleReset}
                  useLoadingState
                  topMargin={0} />
    </Container>
  );
};

export default UsersFilter;
