// @flow strict
import styled, { type StyledComponent } from 'styled-components';

const ColorPreview: StyledComponent<{ color: string }, void, HTMLDivElement> = styled.div(({ color }) => `
  height: 2rem;
  width: 2rem;
  margin-right: 0.4rem;

  background-color: ${color};
  border-radius: 4px;
  border: 1px solid rgba(0,126,255,.24);
`);

export default ColorPreview;
