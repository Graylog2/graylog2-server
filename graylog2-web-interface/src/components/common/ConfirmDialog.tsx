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
import PropTypes from 'prop-types';

import { Modal } from 'components/bootstrap';
import ModalSubmit from 'components/common/ModalSubmit';

/**
 * Component that displays a confirmation dialog box that the user can
 * cancel or confirm.
 */
const ConfirmDialog = ({
  show,
  title,
  children,
  onCancel,
  onConfirm,
  btnConfirmDisabled,
  btnConfirmText,
  hideCancelButton,
}) => {
  const onHide = hideCancelButton ? onConfirm : onCancel;

  return (
    <Modal show={show} onHide={onHide}>
      <Modal.Header closeButton>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        {children}
      </Modal.Body>

      <Modal.Footer>
        <ModalSubmit onCancel={onCancel}
                     onSubmit={onConfirm}
                     submitButtonType="button"
                     disabledSubmit={btnConfirmDisabled}
                     submitButtonText={btnConfirmText}
                     displayCancel={hideCancelButton} />
      </Modal.Footer>
    </Modal>
  );
};

ConfirmDialog.propTypes = {
  /** Indicates whether the dialog should be shown by default or not. */
  show: PropTypes.bool,
  /** Indicates whether the dialog should render the cancel button by default or not. */
  hideCancelButton: PropTypes.bool,
  /** Title to use in the modal. */
  title: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.element,
  ]).isRequired,
  /** Text or element to use in the confirmation button. */
  btnConfirmText: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.element,
  ]),
  /** Indicates whether the confirm button should be disabled or not. */
  btnConfirmDisabled: PropTypes.bool,
  /** Function to call when the action is not confirmed. The function does not receive any arguments. */
  onCancel: PropTypes.func,
  /**
   * Function to call when the action is confirmed. The function receives a callback function to close the modal
   * dialog box as first argument.
   */
  onConfirm: PropTypes.func.isRequired,
  /**
   * React elements to display in the modal body. This should be the information the user has
   * to confirm in order to proceed with the operation.
   */
  children: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.element,
  ]).isRequired,
};

ConfirmDialog.defaultProps = {
  btnConfirmText: 'Confirm',
  btnConfirmDisabled: false,
  show: false,
  hideCancelButton: false,
  onCancel: () => {},
};

export default ConfirmDialog;
