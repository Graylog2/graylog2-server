// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';

const Wrapper: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

type Props = {
  children: String,
};

/**
 * Component that signals text overflow to users by using an ellipsis.
 * The parent component needs a concrete width.
 */
const TextOverflowEllipsis = ({ children }: Props) => (
  <Wrapper>
    {children}
  </Wrapper>
);

export default TextOverflowEllipsis;
