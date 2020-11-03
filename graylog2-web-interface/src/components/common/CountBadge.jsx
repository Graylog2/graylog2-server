// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';

const Badge: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => `
  background-color: ${theme.colors.variant.light.info};
`);

type Props = {
  children: React.Node;
};

const CountBadge = ({ children }: Props) => (
  <Badge className="badge">{children}</Badge>
);

export default CountBadge;
