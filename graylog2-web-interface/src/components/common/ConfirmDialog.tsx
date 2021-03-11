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

import { Modal, Button } from 'components/graylog';

/**
 * Component that displays a confirmation dialog box that the user can
 * cancel or confirm.
 */
const ConfirmDialog = ({
  show,
  onDialogHide,
  onDialogOpen,
  onDialogClose,
  title,
  children,
  onCancel,
  onConfirm,
  btnCancelDisabled,
  btnConfirmDisabled,
  btnCancelText,
  btnConfirmText,
}) => {
  return (
    <Modal show={show} onHide={onDialogHide} onEnter={onDialogOpen} onExit={onDialogClose}>
      <Modal.Header closeButton>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        {children}
      </Modal.Body>

      <Modal.Footer>
        <Button type="button" onClick={onCancel} disabled={btnCancelDisabled}>{btnCancelText}</Button>
        <Button type="button" onClick={onConfirm} bsStyle="primary" disabled={btnConfirmDisabled}>{btnConfirmText}</Button>
      </Modal.Footer>
    </Modal>
  );
};

ConfirmDialog.propTypes = {
  /** Indicates whether the dialog should be shown by default or not. */
  show: PropTypes.bool,
  /** Title to use in the modal. */
  title: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.element,
  ]).isRequired,
  /** Text to use in the cancel button. */
  btnCancelText: PropTypes.string,
  /** Text to use in the confirmation button. */
  btnConfirmText: PropTypes.string,
  /** Indicates whether the cancel button should be disabled or not. */
  btnCancelDisabled: PropTypes.bool,
  /** Indicates whether the confirm button should be disabled or not. */
  btnConfirmDisabled: PropTypes.bool,
  /** Function to call when the modal is hidden. The function does not receive any arguments. */
  onDialogHide: PropTypes.func,
  /** Function to call when the modal is opened. The function does not receive any arguments. */
  onDialogOpen: PropTypes.func,
  /** Function to call when the modal is closed. The function does not receive any arguments. */
  onDialogClose: PropTypes.func,
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
  btnCancelText: 'Cancel',
  btnConfirmText: 'Confirm',
  btnConfirmDisabled: false,
  btnCancelDisabled: false,
  show: false,
  onCancel: () => {},
  onDialogHide: () => {},
  onDialogOpen: () => {},
  onDialogClose: () => {},
};

export default ConfirmDialog;
