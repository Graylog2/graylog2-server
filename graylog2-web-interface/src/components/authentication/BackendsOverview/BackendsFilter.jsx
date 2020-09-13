// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { SearchForm } from 'components/common';
import type { ThemeInterface } from 'theme';
import type { PaginatedBackends } from 'actions/authentication/AuthenticationActions';

import BackendsQueryHelper from './BackendsQueryHelper.jsx';

type Props = {
  onSearch: (query: string) => Promise<?PaginatedBackends>,
  onReset: () => Promise<?PaginatedBackends>,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  margin-bottom: 10px;
`;

const BackendsFilter = ({ onSearch, onReset }: Props) => {
  const _handleSearch = (newQuery, resetLoading) => onSearch(newQuery).then(resetLoading);
  const queryHelper = <BackendsQueryHelper />;

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

export default BackendsFilter;
