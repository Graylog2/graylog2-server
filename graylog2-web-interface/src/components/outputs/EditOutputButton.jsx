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

import { Button } from 'components/bootstrap';
import { ConfigurationForm } from 'components/configurationforms';

class EditOutputButton extends React.Component {
  static propTypes = {
    output: PropTypes.object.isRequired,
    disabled: PropTypes.bool,
    getTypeDefinition: PropTypes.func.isRequired,
    onUpdate: PropTypes.func,
  };

  static defaultProps = {
    disabled: false,
    onUpdate: () => {},
  };

  constructor(props) {
    super(props);

    this.configurationForm = React.createRef();

    this.state = {
      typeDefinition: undefined,
    };

    this.handleClick = this.handleClick.bind(null, props.output);
  }

  handleClick = () => {
    const { getTypeDefinition, output } = this.props;

    getTypeDefinition(output.type, (definition) => {
      this.setState({ typeDefinition: definition.requested_configuration });

      if (this.configurationForm.current) {
        this.configurationForm.current.open();
      }
    });
  };

  _handleSubmit = (data) => {
    const { onUpdate, output } = this.props;

    onUpdate(output, data);
  };

  render() {
    const { typeDefinition } = this.state;
    const { disabled, output } = this.props;
    let configurationForm;

    if (typeDefinition) {
      configurationForm = (
        <ConfigurationForm ref={this.configurationForm}
                           key={`configuration-form-output-${output.id}`}
                           configFields={typeDefinition}
                           title={`Editing Output ${output.title}`}
                           typeName={output.type}
                           titleHelpText="Select a name of your new output that describes it."
                           submitAction={this._handleSubmit}
                           submitButtonText="Update output"
                           values={output.configuration}
                           titleValue={output.title} />
      );
    }

    return (
      <span>
        <Button disabled={disabled} onClick={this.handleClick}>
          Edit
        </Button>
        {configurationForm}
      </span>
    );
  }
}

export default EditOutputButton;
