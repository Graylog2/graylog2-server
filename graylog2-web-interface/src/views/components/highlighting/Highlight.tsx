import * as React from 'react';
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

type BackgroundColorProps = {
  theme: DefaultTheme,
  $color: string,
};

const BackgroundColor = styled.div(({ theme, $color }: BackgroundColorProps) => css`
  background-color: ${$color};
  color: ${theme.utils.contrastingColor($color)};
  width: fit-content;
`);

type Props = {
  color: string,
};

const Highlight = ({ children, color }: React.PropsWithChildren<Props>) => <BackgroundColor $color={color}>{children}</BackgroundColor>;

export default Highlight;
