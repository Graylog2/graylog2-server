/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { Popover as MantinePopover } from '@mantine/core';
import styled, { css, useTheme } from 'styled-components';

const Popover = (props: React.ComponentProps<typeof MantinePopover>) => {
  const theme = useTheme();
  // eslint-disable-next-line react/prop-types
  const arrowBackground = (!props.position || props.position.startsWith('bottom'))
    ? theme.colors.variant.lightest.default
    : theme.colors.global.contentBackground;

  const styles = () => ({
    dropdown: {
      backgroundColor: theme.colors.global.contentBackground,
      borderColor: theme.colors.variant.light.default,
      padding: 0,
    },
    arrow: {
      borderColor: theme.colors.variant.light.default,
      backgroundColor: arrowBackground,
    },
  });

  return <MantinePopover styles={styles} zIndex={1032} {...props} />;
};

type DropdownProps = Omit<React.ComponentProps<typeof MantinePopover.Dropdown>, 'title'> & {
  title?: React.ReactNode,
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

const Dropdown = ({ title, children, ...rest }: DropdownProps) => (
  <MantinePopover.Dropdown {...rest}>
    {title && (
      <Title>
        {title}
      </Title>
    )}

    <Children>
      {children}
    </Children>
  </MantinePopover.Dropdown>
);

Popover.Target = MantinePopover.Target;
Popover.Dropdown = Dropdown;

export default Popover;
