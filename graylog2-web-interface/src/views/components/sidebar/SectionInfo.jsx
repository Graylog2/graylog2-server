// @flow strict
import styled, { type StyledComponent } from 'styled-components';
import { type ThemeInterface } from 'theme';

const Title: StyledComponent<{}, ThemeInterface, HTMLParagraphElement> = styled.p(({ theme }) => `
  color: ${theme.colors.gray[60]};
`);

export default Title;
