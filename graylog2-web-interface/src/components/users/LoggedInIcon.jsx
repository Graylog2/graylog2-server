// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import { Icon } from 'components/common';

const Wrapper: StyledComponent<{active?: boolean}, ThemeInterface, HTMLDivElement> = styled.div(({ theme, active }) => `
  color: ${active ? theme.colors.variant.success : theme.colors.variant.default};
`);

const LoggedInIcon = ({ active, ...rest }: { active: boolean }) => (
  <Wrapper active={active}>
    <Icon {...rest} name="circle" />
  </Wrapper>
);

export default LoggedInIcon;
