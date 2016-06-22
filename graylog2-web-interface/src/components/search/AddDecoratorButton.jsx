import React from 'react';
import Reflux from 'reflux';
import jQuery from 'jquery';

import { ConfigurationForm } from 'components/configurationforms';
import { Select, Spinner } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
const DecoratorsStore = StoreProvider.getStore('Decorators');

import ActionsProvider from 'injection/ActionsProvider';
const DecoratorsActions = ActionsProvider.getActions('Decorators');

const AddDecoratorButton = React.createClass({
  propTypes: {
    stream: React.PropTypes.string,
  },
  mixins: [Reflux.connect(DecoratorsStore)],
  getInitialState() {
    return {
      typeDefinition: {},
    };
  },
  shouldComponentUpdate(nextProps, nextState) {
    return (this.props !== nextProps) || (this.state !== nextState);
  },

  _formatDecoratorType(typeDefinition, typeName) {
    return { value: typeName, label: typeDefinition.name };
  },
  _handleCancel() {
    this.refs.select.clearValue();
  },
  _handleSubmit(data) {
    const request = {
      stream: this.props.stream,
      type: data.type,
      config: data.configuration,
    };
    DecoratorsActions.create(request);
    this.setState({typeName: this.PLACEHOLDER});
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
      <ConfigurationForm ref="configurationForm"
                         key="configuration-form-output" configFields={this.state.typeDefinition.requested_configuration}
                         title={`Create new ${this.state.typeDefinition.name}`}
                         typeName={this.state.typeName} includeTitleField={false}
                         submitAction={this._handleSubmit} cancelAction={this._handleCancel} /> : null);
    return (
      <div className="form-inline" style={{ margin: '4px' }}>
        <div className="form-group">
          <div className="form-group" style={{ width: 300 }}>
            <Select ref="select"
                    placeholder="Select decorator"
                    onValueChange={this._onTypeChange}
                    options={decoratorTypes}
                    matchProp="label"
                    value={this.state.typeName} />
          </div>
          {' '}
          <button className="btn btn-success form-control" disabled={!this.state.typeName}
                  onClick={this._openModal}>Add</button>

        </div>
        {configurationForm}
      </div>
    );
  },
});

export default AddDecoratorButton;
