import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import jQuery from 'jquery';
import PureRenderMixin from 'react-addons-pure-render-mixin';

import { Button } from 'components/graylog';
import { ConfigurationForm } from 'components/configurationforms';
import { Select, Spinner } from 'components/common';

import StoreProvider from 'injection/StoreProvider';

import ActionsProvider from 'injection/ActionsProvider';

// eslint-disable-next-line import/no-webpack-loader-syntax
import DecoratorStyles from '!style!css!components/search/decoratorStyles.css';

const DecoratorsStore = StoreProvider.getStore('Decorators');
const DecoratorsActions = ActionsProvider.getActions('Decorators');

const AddDecoratorButton = createReactClass({
  displayName: 'AddDecoratorButton',

  propTypes: {
    nextOrder: PropTypes.number.isRequired,
    stream: PropTypes.string.isRequired,
    disabled: PropTypes.bool,
  },

  mixins: [Reflux.connect(DecoratorsStore), PureRenderMixin],

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
    const { stream, nextOrder } = this.props;

    const request = {
      stream,
      type: data.type,
      config: data.configuration,
      order: nextOrder,
    };
    DecoratorsActions.create(request);
    this.setState({ typeName: this.PLACEHOLDER });
  },

  _openModal() {
    this.configurationForm.open();
  },

  _onTypeChange(decoratorType) {
    const { types } = this.state;

    this.setState({ typeName: decoratorType });
    if (types[decoratorType]) {
      this.setState({ typeDefinition: types[decoratorType] });
    } else {
      this.setState({ typeDefinition: {} });
    }
  },

  render() {
    const { types, typeDefinition, typeName } = this.state;
    const { disabled } = this.props;

    if (!types) {
      return <Spinner />;
    }
    const decoratorTypes = jQuery.map(types, this._formatDecoratorType);
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
                  options={decoratorTypes}
                  matchProp="label"
                  disabled={disabled}
                  value={typeName} />
        </div>
        <Button bsStyle="success" disabled={!typeName || disabled} onClick={this._openModal}>Apply</Button>
        {typeName && configurationForm}
      </div>
    );
  },
});

export default AddDecoratorButton;
