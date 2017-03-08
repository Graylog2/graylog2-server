import React, { PropTypes } from 'react';

import { BootstrapModalForm, Input } from 'components/bootstrap';

import StoreProvider from 'injection/StoreProvider';
const InputStaticFieldsStore = StoreProvider.getStore('InputStaticFields');

const StaticFieldForm = React.createClass({
  propTypes: {
    input: PropTypes.object.isRequired,
  },
  open() {
    this.refs.modal.open();
  },
  _addStaticField() {
    const fieldName = this.refs.fieldName.getValue();
    const fieldValue = this.refs.fieldValue.getValue();

    InputStaticFieldsStore.create(this.props.input, fieldName, fieldValue).then(() => this.refs.modal.close());
  },
  render() {
    return (
      <BootstrapModalForm ref="modal" title="Add static field" submitButtonText="Add field"
                          onSubmitForm={this._addStaticField}>
        <p>Define a static field that is added to every message that comes in via this input. The field is not
          overwritten If the message already has that key. Key must only contain alphanumeric characters or
          underscores and not be a reserved field.</p>
        <Input ref="fieldName" type="text" id="field-name" label="Field name" className="validatable"
               data-validate="alphanum_underscore" required autoFocus />
        <Input ref="fieldValue" type="text" id="field-value" label="Field value" required />
      </BootstrapModalForm>
    );
  },
});

export default StaticFieldForm;
