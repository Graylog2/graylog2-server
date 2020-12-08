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

import { Button } from 'components/graylog';
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
  }

  constructor(props) {
    super(props);

    this.state = {
      typeDefinition: undefined,
    };

    this.handleClick = this.handleClick.bind(null, props.output);
  }

  handleClick = () => {
    const { getTypeDefinition, output } = this.props;

    getTypeDefinition(output.type, (definition) => {
      this.setState({ typeDefinition: definition.requested_configuration });
      this.configurationForm.open();
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
        <ConfigurationForm ref={(form) => { this.configurationForm = form; }}
                           key={`configuration-form-output-${output.id}`}
                           configFields={typeDefinition}
                           title={`Editing Output ${output.title}`}
                           typeName={output.type}
                           helpBlock="Select a name of your new output that describes it."
                           submitAction={this._handleSubmit}
                           values={output.configuration}
                           titleValue={output.title} />
      );
    }

    return (
      <span>
        <Button disabled={disabled} bsStyle="info" onClick={this.handleClick}>
          Edit
        </Button>
        {configurationForm}
      </span>
    );
  }
}

export default EditOutputButton;
