import React, { Component, PropTypes } from 'react';
import ReactDOM from 'react-dom';
import { Modal, Button } from 'react-bootstrap';
import $ from 'jquery';

import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

import { validate } from 'legacy/validations.js';

/**
 * Encapsulates a form element inside a bootstrap modal, hiding some custom logic that this kind of component
 * has, and providing form validation using HTML5 and our custom validation.
 */
class BootstrapModalForm extends Component {
  constructor(props) {
    super(props);

    this.open = this.open.bind(this);
    this.close = this.close.bind(this);
    this._submit = this._submit.bind(this);
    this._onModalCancel = this._onModalCancel.bind(this);
  }

  _onModalCancel() {
    if (typeof this.props.onCancel === 'function') {
      this.props.onCancel();
    }

    this.close();
  }

  open() {
    this.refs.modal.open();
  }

  close() {
    this.refs.modal.close();
  }

  _submit(event) {
    const formDOMNode = ReactDOM.findDOMNode(this.refs.form);
    const $formDOMNode = $(formDOMNode);

    if ((typeof formDOMNode.checkValidity === 'function' && !formDOMNode.checkValidity()) ||
      (typeof $formDOMNode.checkValidity === 'function' && !$formDOMNode.checkValidity())) {
      event.preventDefault();
      return;
    }

    // Check custom validation for plugin fields
    if (!validate(formDOMNode)) {
      event.preventDefault();
      return;
    }

    if (typeof this.props.onSubmitForm === 'function') {
      event.preventDefault();
      this.props.onSubmitForm(event);
    }
  }

  render() {
    const body = (
      <div className="container-fluid">
        {this.props.children}
      </div>
    );

    return (
      <BootstrapModalWrapper ref="modal"
                             onOpen={this.props.onModalOpen}
                             onClose={this.props.onModalClose}
                             onHide={this._onModalCancel}>
        <Modal.Header closeButton>
          <Modal.Title>{this.props.title}</Modal.Title>
        </Modal.Header>
        <form ref="form" onSubmit={this._submit} {...this.props.formProps}>
          <Modal.Body>
            {body}
          </Modal.Body>
          <Modal.Footer>
            <Button type="button" onClick={this._onModalCancel}>{this.props.cancelButtonText}</Button>
            <Button type="submit" disabled={this.props.submitButtonDisabled} bsStyle="primary">{this.props.submitButtonText}</Button>
          </Modal.Footer>
        </form>
      </BootstrapModalWrapper>
    );
  }
}

BootstrapModalForm.propTypes = {
  /* Modal title */
  title: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
  /* Form contents, included in the modal body */
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]).isRequired,
  onModalOpen: PropTypes.func,
  onModalClose: PropTypes.func,
  onSubmitForm: PropTypes.func,
  onCancel: PropTypes.func,
  /* Object with additional props to pass to the form */
  formProps: PropTypes.object,
  /* Text to use in the cancel button. "Cancel" is the default */
  cancelButtonText: PropTypes.string,
  /* Text to use in the submit button. "Submit" is the default */
  submitButtonText: PropTypes.string,
  submitButtonDisabled: PropTypes.bool,
};

BootstrapModalForm.defaultProps = {
  formProps: {},
  cancelButtonText: 'Cancel',
  submitButtonText: 'Submit',
  submitButtonDisabled: false,
};

export default BootstrapModalForm;
