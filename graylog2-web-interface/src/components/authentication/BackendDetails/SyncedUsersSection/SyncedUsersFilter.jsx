// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { SearchForm } from 'components/common';
import UserQueryHelper from 'components/users/UsersQueryHelper';

type Props = {
  onSearch: (query: string) => void,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  margin-bottom: 10px;
`;

const SyncedUsersFilter = ({ onSearch }: Props) => (
  <Container>
    <SearchForm onReset={() => onSearch('')}
                onSearch={onSearch}
                queryHelpComponent={<UserQueryHelper />}
                topMargin={0} />
  </Container>
);

export default SyncedUsersFilter;
