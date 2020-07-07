// @flow strict
import styled, { css } from 'styled-components';
import type StyledComponent from 'styled-components';

const ColorPreview: StyledComponent<{ color: string }, void, HTMLDivElement> = styled.div(({ color }) => css`
  height: 2rem;
  width: 2rem;
  margin-right: 0.4rem;

  background-color: ${color};
  border-radius: 4px;
  border: 1px solid rgba(0, 126, 255, 0.24);
`);

export default ColorPreview;
