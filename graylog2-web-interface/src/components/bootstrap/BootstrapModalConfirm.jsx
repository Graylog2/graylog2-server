import React from 'react';
import PropTypes from 'prop-types';
import { Button, Modal } from 'react-bootstrap';

import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

/**
 * Component that displays a confirmation dialog box that the user can
 * cancel or confirm.
 */
class BootstrapModalConfirm extends React.Component {
  static propTypes = {
    /** Indicates whether the modal should be shown by default or not. */
    showModal: PropTypes.bool,
    /** Title to use in the modal. */
    title: PropTypes.string.isRequired,
    /** Text to use in the cancel button. */
    cancelButtonText: PropTypes.string,
    /** Text to use in the confirmation button. */
    confirmButtonText: PropTypes.string,
    /** Function to call when the modal is opened. The function does not receive any arguments. */
    onModalOpen: PropTypes.func,
    /** Function to call when the modal is closed. The function does not receive any arguments. */
    onModalClose: PropTypes.func,
    /** Function to call when the action is not confirmed. The function does not receive any arguments. */
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

  static defaultProps = {
    showModal: false,
    cancelButtonText: 'Cancel',
    confirmButtonText: 'Confirm',
    onModalOpen: () => {},
    onModalClose: () => {},
    onCancel: () => {},
    onConfirm: () => {},
  };

  onCancel = () => {
    this.props.onCancel();
    this.close();
  };

  onConfirm = () => {
    this.props.onConfirm(this.close);
  };

  open = () => {
    this.modal.open();
  };

  close = () => {
    this.modal.close();
  };

  render() {
    return (
      <BootstrapModalWrapper ref={(c) => { this.modal = c; }}
                             showModal={this.props.showModal}
                             onOpen={this.props.onModalOpen}
                             onClose={this.props.onModalClose}
                             onHide={this.onCancel}>
        <Modal.Header closeButton>
          <Modal.Title>{this.props.title}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {this.props.children}
        </Modal.Body>
        <Modal.Footer>
          <Button type="button" onClick={this.onCancel}>{this.props.cancelButtonText}</Button>
          <Button type="button" onClick={this.onConfirm} bsStyle="primary">{this.props.confirmButtonText}</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  }
}

export default BootstrapModalConfirm;
