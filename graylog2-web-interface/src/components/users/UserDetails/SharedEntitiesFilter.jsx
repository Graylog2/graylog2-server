// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { SearchForm } from 'components/common';

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  margin-bottom: 10px;
`;

const SharedEntitiesFilter = ({ onSearch, onReset }: { onSearch: any, onReset: any }) => (
  <Container>
    <SearchForm onSearch={onSearch}
                onReset={onReset}
                placeholder="Enter query to filter"
                useLoadingState
                topMargin={0} />
  </Container>
);

export default SharedEntitiesFilter;
