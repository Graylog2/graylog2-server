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
import { InputStaticFieldsStore } from 'stores/inputs/InputStaticFieldsStore';

class StaticFieldForm extends React.Component {
  static propTypes = {
    input: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      showModal: false,
    };
  }

  open = () => {
    this.setState({ showModal: true });
  };

  close = () => {
    this.setState({ showModal: false });
  };

  _addStaticField = () => {
    const fieldName = this.fieldName.getValue();
    const fieldValue = this.fieldValue.getValue();

    InputStaticFieldsStore.create(this.props.input, fieldName, fieldValue).then(() => this.close());
  };

  render() {
    return (
      <BootstrapModalForm show={this.state.showModal}
                          title="Add static field"
                          submitButtonText="Add field"
                          onCancel={this.close}
                          onSubmitForm={this._addStaticField}>
        <p>Define a static field that is added to every message that comes in via this input. The field is not
          overwritten If the message already has that key. Key must only contain alphanumeric characters or
          underscores and not be a reserved field.
        </p>
        <Input ref={(fieldName) => { this.fieldName = fieldName; }}
               type="text"
               id="field-name"
               label="Field name"
               required
               pattern="[A-Za-z0-9_]*"
               title="Should consist only of alphanumeric characters and underscores."
               autoFocus />
        <Input ref={(fieldValue) => { this.fieldValue = fieldValue; }}
               type="text"
               id="field-value"
               label="Field value"
               required />
      </BootstrapModalForm>
    );
  }
}

export default StaticFieldForm;
