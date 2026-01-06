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
import type { ModalRootProps } from '@mantine/core';
import { Modal as MantineModal } from '@mantine/core';
import styled, { css } from 'styled-components';

import type { BsSize } from 'components/bootstrap/types';
import zIndices from 'theme/z-indices';

export type ModalSize = 'lg' | 'large' | 'sm' | 'small';

const ModalOverlay = styled(MantineModal.Overlay)`
  z-index: ${zIndices.modalOverlay};
`;

const ModalContent = styled(MantineModal.Content)`
  z-index: ${zIndices.modalBody};
  border-radius: 10px;
`;

const StyledModalFooter = styled(MantineModal.Body)(
  ({ theme }) => css`
    position: sticky;
    bottom: 0;
    background-color: ${theme.colors.global.contentBackground};
    padding: ${theme.spacings.md};
    z-index: ${zIndices.modalBody};
  `,
);

const StyledModalRoot = styled(MantineModal.Root)<{ $scrollInContent: boolean }>(
  ({ theme, $scrollInContent }) => css`
    --mantine-color-body: ${theme.colors.global.contentBackground};
    ${$scrollInContent &&
    css`
      .mantine-Modal-content {
        display: flex;
        flex-direction: column;
      }

      .mantine-Modal-body {
        flex: 1;
        overflow: auto;
        display: flex;
        flex-direction: column;
      }
    `})
  `,
);

const sizeForMantine = (size: BsSize) => {
  switch (size) {
    case 'sm':
    case 'small':
      return 'md';
    case 'lg':
    case 'large':
      return 'xl';
    default:
      return 'lg';
  }
};

type Props = {
  onHide: () => void;
  children: React.ReactNode;
  show?: boolean;
  bsSize?: ModalSize;
  backdrop?: boolean;
  closable?: boolean;
  fullScreen?: boolean;
  scrollInContent?: boolean;
  rootProps?: Partial<Omit<ModalRootProps, 'opened' | 'onClose'>>;
};

const Modal = ({
  onHide,
  show = false,
  children,
  bsSize = undefined,
  backdrop = true,
  closable = true,
  fullScreen = false,
  scrollInContent = false,
  rootProps = {},
}: Props) => (
  <StyledModalRoot
    $scrollInContent={scrollInContent}
    opened={show}
    onClose={onHide}
    size={sizeForMantine(bsSize)}
    trapFocus
    closeOnEscape={closable}
    fullScreen={fullScreen}
    {...(rootProps || {})}>
    {backdrop && <ModalOverlay />}
    <ModalContent>{children}</ModalContent>
  </StyledModalRoot>
);

Modal.Header = ({ children, showCloseButton = true }: { children: React.ReactNode; showCloseButton?: boolean }) => (
  <MantineModal.Header>
    {children}
    {showCloseButton && <MantineModal.CloseButton />}
  </MantineModal.Header>
);

Modal.Title = styled(MantineModal.Title)`
  font-size: ${({ theme }) => theme.fonts.size.h2};
`;

Modal.Body = MantineModal.Body;
Modal.Footer = StyledModalFooter;

export default Modal;
