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
import React from 'react';

import ModalSubmit from 'components/common/ModalSubmit';

import Modal from './Modal';
import BootstrapModalWrapper from './BootstrapModalWrapper';

type Props = {
  showModal: boolean,
  title: string | React.ReactNode,
  confirmButtonText?: string
  cancelButtonDisabled?: boolean
  confirmButtonDisabled?: boolean
  onConfirm: (e: React.BaseSyntheticEvent) => void,
  onCancel: () => void,
  children: React.ReactNode,
};

/**
 * Component that displays a confirmation dialog box that the user can
 * cancel or confirm.
 */
const BootstrapModalConfirm = ({
  showModal,
  title,
  children,
  cancelButtonDisabled = false,
  confirmButtonDisabled = false,
  confirmButtonText = 'Confirm',
  onCancel,
  onConfirm,
  ...restProps
}: Props) => (
  <BootstrapModalWrapper showModal={showModal}
                         onHide={onCancel}
                         role="alertdialog"
                         {...restProps}>
    <Modal.Header closeButton>
      <Modal.Title>{title}</Modal.Title>
    </Modal.Header>

    <Modal.Body>
      {children}
    </Modal.Body>

    <Modal.Footer>
      <ModalSubmit disabledCancel={cancelButtonDisabled}
                   disabledSubmit={confirmButtonDisabled}
                   onCancel={onCancel}
                   onSubmit={onConfirm}
                   submitButtonText={confirmButtonText}
                   submitButtonType="button" />
    </Modal.Footer>
  </BootstrapModalWrapper>
);

export default BootstrapModalConfirm;
