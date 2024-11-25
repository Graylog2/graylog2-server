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
import React from 'react';
import $ from 'jquery';

import { Button } from 'components/bootstrap';
import { ConfigurationForm } from 'components/configurationforms';
import type { RefType } from 'components/configurationforms/ConfigurationForm';

const formatOutputType = (type, typeName) => (<option key={typeName} value={typeName}>{type.name}</option>);

type CreateOutputDropdownProps = {
  getTypeDefinition: (...args: any[]) => void;
  types: { [key: string]: any };
  onSubmit: (...args: any[]) => void;
};

class CreateOutputDropdown extends React.Component<CreateOutputDropdownProps, {
  [key: string]: any;
}> {
  PLACEHOLDER = 'placeholder';

  private configurationForm: React.RefObject<RefType<{}>>;

  constructor(props) {
    super(props);

    this.configurationForm = React.createRef();

    this.state = {
      typeDefinition: [],
      typeName: this.PLACEHOLDER,
    };
  }

  _openModal = () => {
    if (this.state.typeName !== this.PLACEHOLDER && this.state.typeName !== '' && this.configurationForm.current) {
      this.configurationForm.current.open();
    }
  };

  _onTypeChange = (evt) => {
    const outputType = evt.target.value;

    this.setState({ typeName: evt.target.value });

    this.props.getTypeDefinition(outputType, (definition) => {
      this.setState({ typeDefinition: definition.requested_configuration });
    });
  };

  render() {
    const outputTypes = $.map(this.props.types, formatOutputType);

    return (
      <div>
        <div className="form-inline">
          <select id="input-type" defaultValue={this.PLACEHOLDER} value={this.state.typeName} onChange={this._onTypeChange} className="form-control">
            <option value={this.PLACEHOLDER} disabled>Select Output Type</option>
            {outputTypes}
          </select>
          &nbsp;
          <Button bsStyle="success" disabled={this.state.typeName === this.PLACEHOLDER} onClick={this._openModal}>Launch new output</Button>
        </div>

        <ConfigurationForm ref={this.configurationForm}
                           key="configuration-form-output"
                           configFields={this.state.typeDefinition}
                           title="Create new Output"
                           titleHelpText="Select a name of your new output that describes it."
                           typeName={this.state.typeName}
                           submitButtonText="Create output"
                           submitAction={this.props.onSubmit} />
      </div>
    );
  }
}

export default CreateOutputDropdown;
