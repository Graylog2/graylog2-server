import * as React from 'react';
import { Popover as MantinePopover } from '@mantine/core';
import styled, { css, useTheme } from 'styled-components';

const Popover = (props: React.ComponentProps<typeof MantinePopover>) => {
  const theme = useTheme();

  const styles = () => ({
    dropdown: {
      backgroundColor: theme.colors.global.contentBackground,
      borderColor: theme.colors.variant.light.default,
      padding: 0,
    },
    arrow: {
      borderColor: theme.colors.variant.light.default,
      backgroundColor: theme.colors.variant.lightest.default,
    },
  });

  return <MantinePopover styles={styles} zIndex={1032} {...props} />;
};

type DropdownProps = React.ComponentProps<typeof MantinePopover.Dropdown> & {
  title: string,
}

const Children = styled.div(({ theme }) => css`
  padding: ${theme.spacings.sm} ${theme.spacings.md}
`);

const Title = styled.h4(({ theme }) => css`
  background-color: ${theme.colors.variant.lightest.default};
  color: ${theme.colors.variant.darkest.default};
  padding: ${theme.spacings.sm} ${theme.spacings.md};
  font-size: ${theme.fonts.size.body};
`);

const Dropdown = ({ title, children }: DropdownProps) => (
  <MantinePopover.Dropdown>
    <Title>
      {title}
    </Title>

    <Children>
      {children}
    </Children>
  </MantinePopover.Dropdown>
);

Popover.Target = MantinePopover.Target;
Popover.Dropdown = Dropdown;

export default Popover;
