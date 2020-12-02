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
import jQuery from 'jquery';
import styled from 'styled-components';
import ObjectID from 'bson-objectid';

import { ConfigurationForm } from 'components/configurationforms';
import { Select } from 'components/common';

// eslint-disable-next-line import/no-webpack-loader-syntax
import InlineForm from './InlineForm';
import PopoverHelp from './PopoverHelp';

import DecoratorStyles from '!style!css!./decoratorStyles.css';

const ConfigurationFormContainer = styled.div`
  margin-bottom: 10px;
  margin-top: 10px;
  margin-left: 5px;
  display: inline-block;
  border-style: solid;
  border-color: lightgray;
  border-radius: 5px;
  border-width: 1px;
  padding: 10px;
  background: white;
`;

class AddDecoratorButton extends React.Component {
  static propTypes = {
    decoratorTypes: PropTypes.object.isRequired,
    nextOrder: PropTypes.number.isRequired,
    stream: PropTypes.string,
    disabled: PropTypes.bool,
    onCreate: PropTypes.func.isRequired,
    showHelp: PropTypes.bool,
  };

  static defaultProps = {
    disabled: false,
    showHelp: true,
    stream: null,
  };

  constructor(props) {
    super(props);
    this.state = {};
  }

  _formatDecoratorType = (typeDefinition, typeName) => {
    return { value: typeName, label: typeDefinition.name };
  };

  _handleCancel = () => this.setState({ typeName: undefined, typeDefinition: undefined });

  _handleSubmit = (data) => {
    const { stream, nextOrder, onCreate } = this.props;

    const request = {
      id: ObjectID().toString(),
      stream,
      type: data.type,
      config: data.configuration,
      order: nextOrder,
    };

    onCreate(request);
    this.setState({ typeName: this.PLACEHOLDER });
  };

  _openModal = () => this.configurationForm.open();

  _onTypeChange = (decoratorType) => {
    const { decoratorTypes } = this.props;

    this.setState({ typeName: decoratorType });

    if (decoratorTypes[decoratorType]) {
      this.setState({ typeDefinition: decoratorTypes[decoratorType] });
    } else {
      this.setState({ typeDefinition: {} });
    }
  };

  render() {
    const { typeDefinition, typeName } = this.state;
    const { decoratorTypes, disabled, showHelp = true } = this.props;

    const decoratorTypeOptions = jQuery.map(decoratorTypes, this._formatDecoratorType);
    const wrapperComponent = InlineForm();
    const configurationForm = (typeName !== this.PLACEHOLDER
      ? (
        <ConfigurationForm ref={(elem) => { this.configurationForm = elem; }}
                           key="configuration-form-output"
                           configFields={typeDefinition.requested_configuration}
                           title={`Create new ${typeDefinition.name}`}
                           typeName={typeName}
                           includeTitleField={false}
                           wrapperComponent={wrapperComponent}
                           submitAction={this._handleSubmit}
                           cancelAction={this._handleCancel} />
      ) : null);

    return (
      <>
        <div className={`${DecoratorStyles.decoratorBox} ${DecoratorStyles.addDecoratorButtonContainer}`}>
          <div className={DecoratorStyles.addDecoratorSelect}>
            <Select ref={(select) => { this.select = select; }}
                    placeholder="Select decorator"
                    onChange={this._onTypeChange}
                    options={decoratorTypeOptions}
                    matchProp="label"
                    disabled={disabled}
                    value={typeName} />
          </div>
        </div>
        {showHelp && <PopoverHelp />}

        {typeName && <ConfigurationFormContainer>{configurationForm}</ConfigurationFormContainer>}
      </>
    );
  }
}

export default AddDecoratorButton;
