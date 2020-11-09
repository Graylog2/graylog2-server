// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';

const StyledSpan: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => `
  color: ${theme.colors.gray[60]};
  font-weight: normal;
`);

const InputOptionalInfo = () => (
  <StyledSpan>(Opt.)</StyledSpan>
);

export default InputOptionalInfo;
