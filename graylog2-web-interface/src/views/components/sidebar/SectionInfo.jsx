// @flow strict
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';

const Title: StyledComponent<{}, ThemeInterface, HTMLParagraphElement> = styled.p(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  margin-bottom: 15px;
`);

export default Title;
