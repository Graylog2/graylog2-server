import * as React from 'react';
import { Drawer as MantineDrawer } from '@mantine/core';
import styled, { css } from 'styled-components';

const StyledDrawer = styled(MantineDrawer)(({ theme }) => css`
  .mantine-Drawer-content, .mantine-Drawer-header {
    background-color: ${theme.colors.global.contentBackground};
  }
`);
type Props = Pick<React.ComponentProps<typeof MantineDrawer>, 'opened' | 'onClose' | 'position' | 'size' | 'children' | 'title'>;
const Drawer = ({ title, ...props }: Props) => <StyledDrawer offset={15} padding="lg" radius={5} zIndex={1032} title={<h1>{title}</h1>} {...props} />;

export default Drawer;
