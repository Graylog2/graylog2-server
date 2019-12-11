// @flow strict
import * as React from 'react';

import { validate } from 'legacy/validations';
import { Button } from 'components/graylog/index';

type Props = {
  children: React.Node,
  disabled: boolean,
  onCancel: () => void,
  onSubmitForm: any => void,
};

const InlineForm = (submitTitle: string = 'Create'): React.ComponentType<Props> => React.forwardRef(
  ({ children, disabled, onCancel, onSubmitForm }: Props, ref) => {
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
      <form onSubmit={onSubmit} ref={ref}>
        {children}
        <Button type="submit" bsStyle="success" disabled={disabled}>{submitTitle}</Button>{' '}
        <Button type="button" disabled={disabled} onClick={onCancel}>Cancel</Button>
      </form>
    );
  },
);


export default InlineForm;
