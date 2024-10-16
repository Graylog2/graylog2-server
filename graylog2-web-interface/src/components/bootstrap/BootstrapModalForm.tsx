import React, { useMemo, useRef } from 'react';
import $ from 'jquery';
import isString from 'lodash/isString';

import ModalSubmit from 'components/common/ModalSubmit';

import Modal from './Modal';
import BootstrapModalWrapper from './BootstrapModalWrapper';

type Props = {
  backdrop?: boolean | 'static' | undefined
  submitButtonDisabled?: boolean
  formProps?: object
  bsSize?: 'lg' | 'large' | 'sm' | 'small'
  show: boolean,
  submitButtonText?: string
  onSubmitForm?: (event) => void
  onCancel: () => void,
  title: string | React.ReactNode,
  children: React.ReactNode,
  modalTitle?: string | undefined,
};

/**
 * Encapsulates a form element inside a bootstrap modal, hiding some custom logic that this kind of component
 * has, and providing form validation using HTML5 and our custom validation.
 */
const BootstrapModalForm = ({
  backdrop,
  submitButtonDisabled = false,
  formProps = {},
  bsSize,
  show,
  submitButtonText = 'Submit',
  onSubmitForm,
  onCancel,
  title,
  children,
  modalTitle,
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

  const _title = useMemo<string | undefined>(() => (isString(title) ? title : modalTitle), [modalTitle, title]);

  return (
    <BootstrapModalWrapper bsSize={bsSize}
                           showModal={show}
                           backdrop={backdrop}
                           onHide={onCancel}
                           title={_title}
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

export default BootstrapModalForm;
