import * as React from 'react';
import { forwardRef } from 'react';
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';
import { Button as MantineButton } from '@mantine/core';
import type { ButtonProps } from '@mantine/core';
import type { ComponentPropsWithoutRef, ComponentProps } from 'react';

type StyledMantineButtonProps = ComponentProps<'button'> & ButtonProps & {
  theme: DefaultTheme,
};

const StyledButton = styled(MantineButton)<React.PropsWithChildren<StyledMantineButtonProps>>(({ theme }: StyledMantineButtonProps) => css`
  ${theme.components.button}
`);

interface HTMLButtonProps extends ComponentPropsWithoutRef<'button'> {
  type?: 'submit' | 'button' | 'reset';
  children: React.ReactNode;
}

interface ReactRouterButtonProps {
  children: React.ReactNode;
  component: React.ReactElement;
  to: string;
}

export type CustomButtonProps = HTMLButtonProps | ReactRouterButtonProps | StyledMantineButtonProps;

const Button = forwardRef<HTMLButtonElement, CustomButtonProps>(({ children, ...otherProps }: CustomButtonProps, ref) => {
  return (
    <StyledButton {...otherProps} ref={ref}>
      {children}
    </StyledButton>
  );
});

Button.defaultProps = {
  type: 'button',
};

export default Button;
