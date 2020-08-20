// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { SearchForm } from 'components/common';
import type { PaginatedListType } from 'stores/roles/AuthzRolesStore';

import RolesQueryHelper from '../RolesQueryHelper';

type Props = {
  onSearch: (query: string) => Promise<?PaginatedListType>,
  onReset: () => Promise<?PaginatedListType>,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  margin-bottom: 10px;
`;

const RolesFilter = ({ onSearch, onReset }: Props) => {
  const _handleSearch = (newQuery, resetLoading) => onSearch(newQuery).then(resetLoading);

  return (
    <Container>
      <SearchForm onSearch={_handleSearch}
                  onReset={onReset}
                  useLoadingState
                  queryHelpComponent={<RolesQueryHelper />}
                  topMargin={0} />
    </Container>
  );
};

export default RolesFilter;
