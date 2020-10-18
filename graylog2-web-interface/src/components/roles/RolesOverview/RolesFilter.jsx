// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { SearchForm } from 'components/common';
import type { Pagination } from 'stores/PaginationTypes';

import RolesQueryHelper from '../RolesQueryHelper';

type Props = {
  onSearch: (query: $PropertyType<Pagination, 'query'>) => void,
  onReset: () => void,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  margin-bottom: 10px;
`;

const RolesFilter = ({ onSearch, onReset }: Props) => (
  <Container>
    <SearchForm onSearch={onSearch}
                onReset={onReset}
                queryHelpComponent={<RolesQueryHelper />}
                topMargin={0} />
  </Container>
);

export default RolesFilter;
