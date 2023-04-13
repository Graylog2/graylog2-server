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
import React, { useRef } from 'react';
import $ from 'jquery';

import ModalSubmit from 'components/common/ModalSubmit';
import StringUtils from 'util/StringUtils';

import Modal from './Modal';
import BootstrapModalWrapper from './BootstrapModalWrapper';

type Props = {
  backdrop: boolean|'static'|undefined,
  submitButtonDisabled: boolean,
  formProps: object,
  bsSize: 'lg'|'large'|'sm'|'small',
  show: boolean,
  submitButtonText: string,
  onSubmitForm: (event) => void,
  onCancel: () => void,
  title: string|React.ReactNode,
  children: React.ReactNode,
};

/**
 * Encapsulates a form element inside a bootstrap modal, hiding some custom logic that this kind of component
 * has, and providing form validation using HTML5 and our custom validation.
 */
const BootstrapModalForm = ({
  backdrop,
  submitButtonDisabled,
  formProps,
  bsSize,
  show,
  submitButtonText,
  onSubmitForm,
  onCancel,
  title,
  children,
  ...restProps
}: Props) => {
  const form = useRef(null);

  const submit = (event) => {
    const formDOMNode = form.current;
    const $formDOMNode = $(formDOMNode) as any;

    if ((typeof formDOMNode.checkValidity === 'function' && !formDOMNode.checkValidity())
      || (typeof $formDOMNode.checkValidity === 'function' && !$formDOMNode.checkValidity())) {
      event.preventDefault();

      return;
    }

    if (typeof onSubmitForm === 'function') {
      event.preventDefault();
      onSubmitForm(event);
    }
  };

  const body = (
    <div className="container-fluid">
      {children}
    </div>
  );

  return (
    <BootstrapModalWrapper bsSize={bsSize}
                           showModal={show}
                           backdrop={backdrop}
                           onHide={onCancel}
                           data-event-element={restProps['data-telemetry-title'] || StringUtils.getRecursiveChildText(title)}
                           {...restProps}>
      <Modal.Header closeButton>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <form ref={form}
            onSubmit={submit}
            {...formProps}
            data-testid="modal-form">
        <Modal.Body>
          {body}
        </Modal.Body>
        <Modal.Footer>
          <ModalSubmit disabledSubmit={submitButtonDisabled}
                       onCancel={onCancel}
                       submitButtonText={submitButtonText} />
        </Modal.Footer>
      </form>
    </BootstrapModalWrapper>
  );
};

BootstrapModalForm.propTypes = {
  backdrop: PropTypes.oneOf([true, false, 'static']),
  bsSize: PropTypes.oneOf(['lg', 'large', 'sm', 'small']),
  /* Modal title */
  title: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
  /* Form contents, included in the modal body */
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]).isRequired,
  onSubmitForm: PropTypes.func,
  onCancel: PropTypes.func.isRequired,
  /* Object with additional props to pass to the form */
  formProps: PropTypes.object,
  /* Text to use in the submit button. "Submit" is the default */
  submitButtonText: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
  submitButtonDisabled: PropTypes.bool,
  show: PropTypes.bool.isRequired,
};

BootstrapModalForm.defaultProps = {
  backdrop: undefined,
  formProps: {},
  submitButtonText: 'Submit',
  submitButtonDisabled: false,
  onSubmitForm: undefined,
  bsSize: undefined,
};

export default BootstrapModalForm;
