// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import Grantee from 'logic/permissions/Grantee';
import { type ThemeInterface } from 'theme';
import { Icon } from 'components/common';

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  display: inline-flex;
  align-items: center;
  justify-content: center;

  height: 30px;
  width: 30px;

  border-radius: 50%;
  background-color: ${theme.colors.gray[80]};
`);

type Props = {
  type: $PropertyType<Grantee, 'type'>,
};

const _iconName = (type) => {
  switch (type) {
    case 'global':
    case 'team':
      return 'users';
    case 'user':
    default:
      return 'user';
  }
};

const GranteeIcon = ({ type, ...rest }: Props) => (
  <Container {...rest}>
    <Icon name={_iconName(type)} />
  </Container>
);

export default GranteeIcon;
