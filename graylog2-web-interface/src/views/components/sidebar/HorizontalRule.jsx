// @flow strict

import styled, { type StyledComponent } from 'styled-components';

const HorizontalRuler: StyledComponent<{ fullWidth?: boolean }, void, HTMLDivElement> = styled.div(({ fullWidth }) => `
  width: 100%;
  padding: ${fullWidth ? '0' : '0 10px'};
  margin: 5px 0 10px 0;

  &::after {
    content: ' ';
    display: block;
    width: 100%;
    border-bottom: 1px solid currentColor;
  }
`);

export default HorizontalRuler;
