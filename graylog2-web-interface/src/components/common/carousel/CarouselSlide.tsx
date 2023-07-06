import * as React from 'react';
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';

export interface CarouselSlideProps extends React.ComponentPropsWithoutRef<'div'> {
  children?: React.ReactNode;

  size?: string | number;

  gap?: number;
}

const StyledSlide = styled.div(({ $size, $gap, theme }: {
  theme: DefaultTheme,
  $size: string | number,
  $gap: number
}) => css`
  flex: 0 0 ${$size ?? '24%'};;
  min-width: 0;
  min-height: 100px;
  margin-right: ${$gap ?? theme.spacings.sm};
  position: relative;
`);

const CarouselSlide = ({ children, size, gap }: CarouselSlideProps) => (
  <StyledSlide $size={size} $gap={gap}>
    {children}
  </StyledSlide>
);

CarouselSlide.defaultProps = {
  children: undefined,
  size: undefined,
  gap: undefined,
};

export default CarouselSlide;
