import PropTypes from 'prop-types';
import React from 'react';
import jQuery from 'jquery';
import styled from 'styled-components';
import ObjectID from 'bson-objectid';

import { ConfigurationForm } from 'components/configurationforms';
import { Select } from 'components/common';

// eslint-disable-next-line import/no-webpack-loader-syntax
import DecoratorStyles from '!style!css!./decoratorStyles.css';
import InlineForm from './InlineForm';
import PopoverHelp from './PopoverHelp';

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
      <React.Fragment>
        <div className={`${DecoratorStyles.decoratorBox} ${DecoratorStyles.addDecoratorButtonContainer}`}>
          <div className={DecoratorStyles.addDecoratorSelect}>
            <Select ref={(select) => { this.select = select; }}
                    placeholder="Select decorator"
                    onChange={this._onTypeChange}
                    options={decoratorTypeOptions}
                    matchProp="label"
                    isClearable
                    disabled={disabled}
                    value={typeName} />
          </div>
        </div>
        {showHelp && <PopoverHelp />}

        {typeName && <ConfigurationFormContainer>{configurationForm}</ConfigurationFormContainer>}
      </React.Fragment>
    );
  }
}

export default AddDecoratorButton;
