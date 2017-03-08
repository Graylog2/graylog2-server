import React from 'react';
import Reflux from 'reflux';
import jQuery from 'jquery';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import { Button } from 'react-bootstrap';

import { ConfigurationForm } from 'components/configurationforms';
import { Select, Spinner } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
const DecoratorsStore = StoreProvider.getStore('Decorators');

import ActionsProvider from 'injection/ActionsProvider';
const DecoratorsActions = ActionsProvider.getActions('Decorators');

import DecoratorStyles from '!style!css!components/search/decoratorStyles.css';

const AddDecoratorButton = React.createClass({
  propTypes: {
    nextOrder: React.PropTypes.number.isRequired,
    stream: React.PropTypes.string,
    disabled: React.PropTypes.bool,
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
    this.refs.select.clearValue();
    this.setState(this.getInitialState());
  },
  _handleSubmit(data) {
    const request = {
      stream: this.props.stream,
      type: data.type,
      config: data.configuration,
      order: this.props.nextOrder,
    };
    DecoratorsActions.create(request);
    this.setState({ typeName: this.PLACEHOLDER });
  },
  _openModal() {
    this.refs.configurationForm.open();
  },
  _onTypeChange(decoratorType) {
    this.setState({ typeName: decoratorType });
    if (this.state.types[decoratorType]) {
      this.setState({ typeDefinition: this.state.types[decoratorType] });
    } else {
      this.setState({ typeDefinition: {} });
    }
  },
  render() {
    if (!this.state.types) {
      return <Spinner />;
    }
    const decoratorTypes = jQuery.map(this.state.types, this._formatDecoratorType);
    const configurationForm = (this.state.typeName !== this.PLACEHOLDER ?
      (<ConfigurationForm ref="configurationForm"
                         key="configuration-form-output" configFields={this.state.typeDefinition.requested_configuration}
                         title={`Create new ${this.state.typeDefinition.name}`}
                         typeName={this.state.typeName} includeTitleField={false}
                         submitAction={this._handleSubmit} cancelAction={this._handleCancel} />) : null);
    return (
      <div className={`${DecoratorStyles.decoratorBox} ${DecoratorStyles.addDecoratorButtonContainer}`}>
        <div className={DecoratorStyles.addDecoratorSelect}>
          <Select ref="select"
                  placeholder="Select decorator"
                  onValueChange={this._onTypeChange}
                  options={decoratorTypes}
                  matchProp="label"
                  disabled={this.props.disabled}
                  value={this.state.typeName} />
        </div>
        <Button bsStyle="success" disabled={!this.state.typeName || this.props.disabled} onClick={this._openModal}>Apply</Button>
        {this.state.typeName && configurationForm}
      </div>
    );
  },
});

export default AddDecoratorButton;
