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
// eslint-disable-next-line no-restricted-imports
import { Modal as MantineModal } from '@mantine/core';
import styled from 'styled-components';

import type { BsSize } from 'components/bootstrap/types';

const ModalOverlay = styled(MantineModal.Overlay)`
  z-index: 1030;
`;

const ModalContent = styled(MantineModal.Content)`
  z-index: 1031;
`;

const sizeForMantine = (size: BsSize) => {
  switch (size) {
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
  bsSize?: 'lg' | 'large';
};

const Modal = ({ onHide, show = false, children, bsSize = undefined }: Props) => (
  <MantineModal.Root opened={show} onClose={onHide} size={sizeForMantine(bsSize)}>
    <ModalOverlay />
    <ModalContent>{children}</ModalContent>
  </MantineModal.Root>
);

Modal.Header = ({ children, closeButton }: { children: React.ReactNode; closeButton: boolean }) => (
  <MantineModal.Header>
    {children}
    {closeButton && <MantineModal.CloseButton />}
  </MantineModal.Header>
);

Modal.Title = styled(MantineModal.Title)`
  font-size: ${({ theme }) => theme.fonts.size.h2};
`;

Modal.Body = MantineModal.Body;
Modal.Footer = MantineModal.Body;

/** @component */
export default Modal;
