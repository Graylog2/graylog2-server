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
import PropTypes from 'prop-types';
import React from 'react';
import $ from 'jquery';

import { Button, Modal } from 'components/graylog';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import { validate } from 'legacy/validations.js';

/**
 * Encapsulates a form element inside a bootstrap modal, hiding some custom logic that this kind of component
 * has, and providing form validation using HTML5 and our custom validation.
 */
class BootstrapModalForm extends React.Component {
  static propTypes = {
    backdrop: PropTypes.oneOf([true, false, 'static']),
    bsSize: PropTypes.oneOf(['lg', 'large', 'sm', 'small']),
    /* Modal title */
    title: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
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
    show: PropTypes.bool,
  };

  static defaultProps = {
    backdrop: undefined,
    formProps: {},
    cancelButtonText: 'Cancel',
    submitButtonText: 'Submit',
    submitButtonDisabled: false,
    onModalOpen: () => {},
    onModalClose: () => {},
    onSubmitForm: undefined,
    onCancel: () => {},
    bsSize: undefined,
    show: false,
  };

  onModalCancel = () => {
    const { onCancel } = this.props;

    onCancel();
    this.close();
  };

  open = () => this.modal.open();

  close = () => this.modal.close();

  submit = (event) => {
    const formDOMNode = this.form;
    const $formDOMNode = $(formDOMNode);

    if ((typeof formDOMNode.checkValidity === 'function' && !formDOMNode.checkValidity())
      || (typeof $formDOMNode.checkValidity === 'function' && !$formDOMNode.checkValidity())) {
      event.preventDefault();

      return;
    }

    // Check custom validation for plugin fields
    if (!validate(formDOMNode)) {
      event.preventDefault();

      return;
    }

    // If function is not given, let the browser continue propagating the submit event
    const { onSubmitForm } = this.props;

    if (typeof onSubmitForm === 'function') {
      event.preventDefault();
      onSubmitForm(event);
    }
  };

  render() {
    const { backdrop, submitButtonDisabled, formProps, bsSize, onModalClose, cancelButtonText, show, submitButtonText, onModalOpen, title, children } = this.props;
    const body = (
      <div className="container-fluid">
        {children}
      </div>
    );

    return (
      <BootstrapModalWrapper ref={(c) => { this.modal = c; }}
                             onOpen={onModalOpen}
                             onClose={onModalClose}
                             bsSize={bsSize}
                             showModal={show}
                             backdrop={backdrop}
                             onHide={this.onModalCancel}>
        <Modal.Header closeButton>
          <Modal.Title>{title}</Modal.Title>
        </Modal.Header>
        <form ref={(c) => { this.form = c; }} onSubmit={this.submit} {...formProps} data-testid="modal-form">
          <Modal.Body>
            {body}
          </Modal.Body>
          <Modal.Footer>
            <Button type="button" onClick={this.onModalCancel}>{cancelButtonText}</Button>
            <Button type="submit" disabled={submitButtonDisabled} bsStyle="primary">{submitButtonText}</Button>
          </Modal.Footer>
        </form>
      </BootstrapModalWrapper>
    );
  }
}

export default BootstrapModalForm;
