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
import ReactDom from 'react-dom';
import { Drawer as MantineDrawer } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import styled, { css } from 'styled-components';

const StyledDrawer = styled(MantineDrawer)(
  ({ theme }) => css`
    .mantine-Drawer-content,
    .mantine-Drawer-header {
      background-color: ${theme.colors.global.contentBackground};
    }

    .mantine-Drawer-content {
      display: flex;
      flex-direction: column;

      &.double {
        flex-basis: calc(var(--drawer-size) * 2);
      }
    }

    .mantine-Drawer-body {
      flex: 1;
    }

    .mantine-Drawer-title {
      width: 94%;
    }
  `,
);

const TitleWrapper = styled.div`
  display: flex;
  align-items: center;
  flex: 1 1 auto;
  overflow: hidden;
  width: 100%;
`;

const Title = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.h1};
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    width: 100%;
    line-height: 1.2;
  `,
);

export const getDrawerPropsByLevel = ({
  parentSize,
  level = 0,
  parentPosition = 'right',
}: {
  parentSize: string;
  level?: number;
  parentPosition?: 'left' | 'right' | 'top' | 'bottom';
}) => ({
  position: parentPosition,
  withOverlay: level === 0,
  styles: parentPosition
    ? { inner: { [parentPosition]: `calc((var(--drawer-size-${parentSize}) + 0.8rem) * ${level})` } }
    : undefined,
  overlayProps: { zIndex: `103${level}` },
});

type Props = Pick<
  React.ComponentProps<typeof MantineDrawer>,
  | 'onClose'
  | 'position'
  | 'size'
  | 'children'
  | 'title'
  | 'closeOnClickOutside'
  | 'offset'
  | 'zIndex'
  | 'withOverlay'
  | 'overlayProps'
  | 'styles'
  | 'transitionProps'
  | 'styles'
  | 'lockScroll'
> & {
  opened?: boolean;
  double?: boolean;
  parentSize?: string;
  level?: number;
  parentPosition?: 'left' | 'right' | 'top' | 'bottom';
};

const Drawer = ({
  title,
  double = false,
  parentSize = undefined,
  level = 0,
  parentPosition = 'right',
  ...props
}: Props) => {
  const [opened, { open, close }] = useDisclosure(false);

  React.useLayoutEffect(() => {
    const timeoutId = setTimeout(() => open(), 80);
    return () => clearTimeout(timeoutId);
  }, [open, close, props.onClose]);

  const handleClose = () => {
    close();
    setTimeout(() => props.onClose?.(), 200);
  };

  return ReactDom.createPortal(
    <StyledDrawer
      opened={opened}
      offset={15}
      padding="lg"
      radius={5}
      zIndex={1032}
      classNames={{ content: double ? 'double' : '' }}
      title={
        <TitleWrapper>
          <Title>{title}</Title>
        </TitleWrapper>
      }
      {...getDrawerPropsByLevel({ parentSize, level, parentPosition })}
      {...props}
      onClose={handleClose}
    />,
    document.body,
  );
};

export default Drawer;
