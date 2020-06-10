// @flow strict
import styled, { type StyledComponent } from 'styled-components';
import { type ThemeInterface } from 'theme';

const HorizontalRuler: StyledComponent<{ fullWidth?: boolean }, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  width: 100%;
  color: ${theme.utils.contrastingColor(theme.colors.gray[10], 'AA')};
  margin: 5px 0 10px 0;

  &::after {
    content: ' ';
    display: block;
    width: 100%;
    border-bottom: 1px solid currentColor;
  }
`);

export default HorizontalRuler;
