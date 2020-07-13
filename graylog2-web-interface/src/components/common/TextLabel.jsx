// @flow strict
import * as React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';

type Props = {
  children: React.Node,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  font-size: ${theme.fonts.size.body};
  font-weight: 700;
`);

const TextLabel = (props: Props) => <Container {...props} />;

export default TextLabel;
