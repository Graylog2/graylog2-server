// @flow strict
import styled, { type StyledComponent, css } from 'styled-components';

import { type ThemeInterface } from 'theme';

const MainDetailsGrid: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => css`
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-column-gap: 45px;

  @media (max-width: ${theme.breakpoints.max.md}) {
    grid-template-columns: 1fr;
  }
`);

export default MainDetailsGrid;
