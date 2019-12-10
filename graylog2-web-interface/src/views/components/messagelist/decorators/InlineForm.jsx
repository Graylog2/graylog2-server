// @flow strict
import * as React from 'react';

import { validate } from 'legacy/validations';
import { Button } from 'components/graylog/index';

const InlineForm = (submitTitle = 'Create') => ({ children, disabled, onCancel, onSubmitForm }) => {
  const onSubmit = (event) => {
    event.stopPropagation();
    const { target: formDOMNode } = event;

    if ((typeof formDOMNode.checkValidity === 'function' && !formDOMNode.checkValidity())) {
      event.preventDefault();
      return;
    }

    // Check custom validation for plugin fields
    if (!validate(formDOMNode)) {
      event.preventDefault();
      return;
    }

    // If function is not given, let the browser continue propagating the submit event
    if (typeof onSubmitForm === 'function') {
      event.preventDefault();
      onSubmitForm(event);
    }
  };
  return (
    <form onSubmit={onSubmit}>
      {children}
      <Button type="submit" bsStyle="success" disabled={disabled}>{submitTitle}</Button>{' '}
      <Button type="button" disabled={disabled} onClick={onCancel}>Cancel</Button>
    </form>
  );
};

InlineForm.propTypes = {};

export default InlineForm;
