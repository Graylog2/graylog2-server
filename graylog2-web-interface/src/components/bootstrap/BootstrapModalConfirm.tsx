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

import ModalSubmit from 'components/common/ModalSubmit';
import StringUtils from 'util/StringUtils';

import Modal from './Modal';
import BootstrapModalWrapper from './BootstrapModalWrapper';

type Props = {
  showModal: boolean,
  title: string|React.ReactNode,
  confirmButtonText: string,
  cancelButtonDisabled: boolean,
  confirmButtonDisabled: boolean,
  onConfirm: () => void,
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
  cancelButtonDisabled,
  confirmButtonDisabled,
  confirmButtonText,
  onCancel,
  onConfirm,
  ...restProps
}: Props) => {
  return (
    <BootstrapModalWrapper showModal={showModal}
                           onHide={onCancel}
                           role="alertdialog"
                           data-event-element={restProps['data-telemetry-title'] || StringUtils.getRecursiveChildText(title)}
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
};

BootstrapModalConfirm.propTypes = {
  /** Control whether the modal is shown or not. Prop updates should trigger opening (if show changes from false to true), respectively closing the modal (if show changes from false to true). */
  showModal: PropTypes.bool.isRequired,
  /** Title to use in the modal. */
  title: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.element,
  ]).isRequired,
  /** Text to use in the confirmation button. */
  confirmButtonText: PropTypes.string,
  /** Indicates whether the cancel button should be disabled or not. */
  cancelButtonDisabled: PropTypes.bool,
  /** Indicates whether the confirm button should be disabled or not. */
  confirmButtonDisabled: PropTypes.bool,
  /** Function to call when the modal is opened. The function does not receive any arguments. */
  onCancel: PropTypes.func.isRequired,
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

BootstrapModalConfirm.defaultProps = {
  confirmButtonText: 'Confirm',
  cancelButtonDisabled: false,
  confirmButtonDisabled: false,
};

export default BootstrapModalConfirm;
