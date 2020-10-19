// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { SearchForm } from 'components/common';

import BackendsQueryHelper from './BackendsQueryHelper.jsx';

type Props = {
  onSearch: (query: string) => void,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  margin-bottom: 10px;
`;

const BackendsFilter = ({ onSearch }: Props) => (
  <Container>
    <SearchForm onReset={() => onSearch('')}
                onSearch={onSearch}
                queryHelpComponent={<BackendsQueryHelper />}
                topMargin={0}
                useLoadingState />
  </Container>
);

export default BackendsFilter;
