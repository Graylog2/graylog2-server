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

import { BootstrapModalForm, Input } from 'components/bootstrap';
import StoreProvider from 'injection/StoreProvider';

const InputStaticFieldsStore = StoreProvider.getStore('InputStaticFields');

class StaticFieldForm extends React.Component {
  static propTypes = {
    input: PropTypes.object.isRequired,
  };

  open = () => {
    this.modal.open();
  };

  _addStaticField = () => {
    const fieldName = this.fieldName.getValue();
    const fieldValue = this.fieldValue.getValue();

    InputStaticFieldsStore.create(this.props.input, fieldName, fieldValue).then(() => this.modal.close());
  };

  render() {
    return (
      <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
                          title="Add static field"
                          submitButtonText="Add field"
                          onSubmitForm={this._addStaticField}>
        <p>Define a static field that is added to every message that comes in via this input. The field is not
          overwritten If the message already has that key. Key must only contain alphanumeric characters or
          underscores and not be a reserved field.
        </p>
        <Input ref={(fieldName) => { this.fieldName = fieldName; }}
               type="text"
               id="field-name"
               label="Field name"
               className="validatable"
               data-validate="alphanum_underscore"
               required
               autoFocus />
        <Input ref={(fieldValue) => { this.fieldValue = fieldValue; }} type="text" id="field-value" label="Field value" required />
      </BootstrapModalForm>
    );
  }
}

export default StaticFieldForm;
