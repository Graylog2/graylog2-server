// @flow strict
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';

const Errors: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  width: 100%;
  margin-top: 3px;
  color: ${theme.colors.variant.danger};
`);

export default Errors;
