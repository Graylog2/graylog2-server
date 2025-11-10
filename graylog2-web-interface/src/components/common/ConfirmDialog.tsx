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
import styled from 'styled-components';

import { Modal } from 'components/bootstrap';
import ModalSubmit from 'components/common/ModalSubmit';

const StyledModal = styled(Modal)`
  z-index: 1070;
`;

type Props = {
  btnConfirmDisabled?: boolean;
  btnConfirmText?: React.ReactNode;
  children: React.ReactNode;
  hideCancelButton?: boolean;
  isAsyncSubmit?: boolean;
  isSubmitting?: boolean;
  onCancel?: () => void;
  onConfirm: () => void;
  show?: boolean;
  submitLoadingText?: string;
  title: string | React.ReactNode;
};

/**
 * Component that displays a confirmation dialog box that the user can
 * cancel or confirm.
 */
const ConfirmDialog = ({
  btnConfirmDisabled = false,
  btnConfirmText = 'Confirm',
  children,
  hideCancelButton = false,
  isAsyncSubmit = undefined,
  isSubmitting = undefined,
  onCancel = () => {},
  onConfirm,
  show = false,
  submitLoadingText = undefined,
  title,
}: Props) => {
  const onHide = hideCancelButton ? onConfirm : onCancel;

  const submit = hideCancelButton ? (
    <ModalSubmit
      autoFocus
      onSubmit={onConfirm}
      submitButtonType="button"
      disabledSubmit={btnConfirmDisabled}
      submitButtonText={btnConfirmText}
    />
  ) : (
    <ModalSubmit
      autoFocus
      disabledSubmit={btnConfirmDisabled}
      displayCancel
      isAsyncSubmit={isAsyncSubmit}
      isSubmitting={isSubmitting}
      onCancel={onCancel}
      onSubmit={onConfirm}
      submitButtonText={btnConfirmText}
      submitButtonType="button"
      submitLoadingText={submitLoadingText}
    />
  );

  return (
    <StyledModal show={show} onHide={onHide}>
      <Modal.Header>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>

      <Modal.Body>{children}</Modal.Body>

      <Modal.Footer>{submit}</Modal.Footer>
    </StyledModal>
  );
};

export default ConfirmDialog;
