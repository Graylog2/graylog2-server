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
import { useLayoutEffect, useRef } from 'react';
import ReactDOM from 'react-dom';
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

const DRAWER_OPEN_DELAY_MS = 80;
const DRAWER_CLOSE_DELAY_MS = 200;

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
  overlayProps: { zIndex: 1030 + level },
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

  const closeTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useLayoutEffect(() => {
    setTimeout(() => open(), DRAWER_OPEN_DELAY_MS);

    return () => {
      if (closeTimeoutRef.current !== null) {
        clearTimeout(closeTimeoutRef.current);
        closeTimeoutRef.current = null;
      }
    };
  }, [open]);

  const handleClose = () => {
    close();
    closeTimeoutRef.current = setTimeout(() => props.onClose?.(), DRAWER_CLOSE_DELAY_MS);
  };

  return ReactDOM.createPortal(
    <StyledDrawer
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
      opened={opened}
      onClose={handleClose}
    />,
    document.body,
  );
};

export default Drawer;
