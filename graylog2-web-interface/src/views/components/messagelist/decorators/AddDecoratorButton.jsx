import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import jQuery from 'jquery';
import PureRenderMixin from 'react-addons-pure-render-mixin';

import { Button } from 'components/graylog';
import { ConfigurationForm } from 'components/configurationforms';
import { Select } from 'components/common';

// eslint-disable-next-line import/no-webpack-loader-syntax
import DecoratorStyles from '!style!css!./decoratorStyles.css';

const AddDecoratorButton = createReactClass({
  displayName: 'AddDecoratorButton',

  propTypes: {
    nextOrder: PropTypes.number.isRequired,
    stream: PropTypes.string.isRequired,
    disabled: PropTypes.bool,
  },

  getDefaultProps() {
    return {
      disabled: false,
    };
  },

  getInitialState() {
    return {
      typeDefinition: {},
    };
  },

  _formatDecoratorType(typeDefinition, typeName) {
    return { value: typeName, label: typeDefinition.name };
  },

  _handleCancel() {
    this.select.clearValue();
    this.setState(this.getInitialState());
  },

  _handleSubmit(data) {
    const { stream, nextOrder, onCreate } = this.props;

    const request = {
      stream,
      type: data.type,
      config: data.configuration,
      order: nextOrder,
    };
    onCreate(request);
    this.setState({ typeName: this.PLACEHOLDER });
  },

  _openModal() {
    this.configurationForm.open();
  },

  _onTypeChange(decoratorType) {
    const { decoratorTypes } = this.props;

    this.setState({ typeName: decoratorType });
    if (decoratorTypes[decoratorType]) {
      this.setState({ typeDefinition: decoratorTypes[decoratorType] });
    } else {
      this.setState({ typeDefinition: {} });
    }
  },

  render() {
    const { typeDefinition, typeName } = this.state;
    const { decoratorTypes, disabled } = this.props;

    const decoratorTypeOptions = jQuery.map(decoratorTypes, this._formatDecoratorType);
    const configurationForm = (typeName !== this.PLACEHOLDER
      ? (
        <ConfigurationForm ref={(elem) => { this.configurationForm = elem; }}
                           key="configuration-form-output"
                           configFields={typeDefinition.requested_configuration}
                           title={`Create new ${typeDefinition.name}`}
                           typeName={typeName}
                           includeTitleField={false}
                           submitAction={this._handleSubmit}
                           cancelAction={this._handleCancel} />
      ) : null);
    return (
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
        <Button bsStyle="success" disabled={!typeName || disabled} onClick={this._openModal}>Create</Button>
        {typeName && configurationForm}
      </div>
    );
  },
});

export default AddDecoratorButton;
