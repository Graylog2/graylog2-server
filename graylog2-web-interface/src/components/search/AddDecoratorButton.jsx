import React from 'react';
import Reflux from 'reflux';
import jQuery from 'jquery';

import { ConfigurationForm } from 'components/configurationforms';
import { Spinner } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
const DecoratorsStore = StoreProvider.getStore('Decorators');

import ActionsProvider from 'injection/ActionsProvider';
const DecoratorsActions = ActionsProvider.getActions('Decorators');

const AddDecoratorButton = React.createClass({
  propTypes: {
    stream: React.PropTypes.string,
  },
  mixins: [Reflux.connect(DecoratorsStore)],
  componentDidMount() {
    DecoratorsActions.available();
  },
  getInitialState() {
    return {
      typeName: this.PLACEHOLDER,
      typeDefinition: {},
    };
  },
  PLACEHOLDER: '=== SELECT === ',
  _formatDecoratorType(typeDefinition, typeName) {
    return (<option key={typeName} value={typeName}>{typeName}</option>);
  },
  _handleCancel() {
    this.setState({typeName: this.PLACEHOLDER});
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
  _onTypeChange(evt) {
    const decoratorType = evt.target.value;
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
                         key="configuration-form-output" configFields={this.state.typeDefinition} title={'Create new Decorator'}
                         typeName={this.state.typeName} includeTitleField={false}
                         submitAction={this._handleSubmit} cancelAction={this._handleCancel} /> : null);
    return (
      <div className="form-inline">
        <div className="form-group">
          <select id="decorator-type" value={this.state.typeName} onChange={this._onTypeChange} className="form-control">
            <option value={this.PLACEHOLDER} disabled>Select Decorator</option>
            {decoratorTypes}
          </select>
          {' '}
          <button className="btn btn-success form-control" disabled={this.state.typeName === this.PLACEHOLDER}
                  onClick={this._openModal}>Add</button>

        </div>
        {configurationForm}
      </div>
    );
  },
});

export default AddDecoratorButton;
