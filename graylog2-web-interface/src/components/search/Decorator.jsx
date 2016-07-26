import React from 'react';
import Reflux from 'reflux';

import { Button, Col, Row } from 'react-bootstrap';

import { Spinner } from 'components/common';
import { ConfigurationForm, ConfigurationWell } from 'components/configurationforms';

import StoreProvider from 'injection/StoreProvider';
const DecoratorsStore = StoreProvider.getStore('Decorators');

import ActionsProvider from 'injection/ActionsProvider';
const DecoratorsActions = ActionsProvider.getActions('Decorators');

import DecoratorStyles from '!style!css!components/search/decoratorStyles.css';

const Decorator = React.createClass({
  propTypes: {
    decorator: React.PropTypes.object.isRequired,
    typeDefinition: React.PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(DecoratorsStore)],
  componentDidMount() {
    DecoratorsActions.available();
  },

  _handleDeleteClick() {
    if (window.confirm('Do you really want to delete this decorator?')) {
      DecoratorsActions.remove(this.props.decorator._id);
    }
  },
  _handleEditClick() {
    this.refs.editForm.open();
  },
  _handleSubmit(data) {
    DecoratorsActions.update(this.props.decorator._id, { type: data.type, config: data.configuration });
  },
  _decoratorTypeNotPresent() {
    return {
      name: <strong>Unknown decorator type</strong>,
    };
  },
  render() {
    if (!this.state.types) {
      return <Spinner />;
    }
    const decorator = this.props.decorator;
    const decoratorType = this.state.types[decorator.type] || this._decoratorTypeNotPresent();
    return (
      <span className={DecoratorStyles.fullWidth}>
        <span className={DecoratorStyles.decoratorBox}>
          <strong>{decoratorType.name}</strong>
          <span>
            <Button bsStyle="primary" bsSize="xsmall" onClick={this._handleDeleteClick}>Delete</Button>
            {' '}
            <Button bsStyle="info" bsSize="xsmall" onClick={this._handleEditClick}>Edit</Button>
          </span>
        </span>
        <ConfigurationWell key={`configuration-well-decorator-${decorator._id}`}
                           id={decorator._id}
                           configuration={decorator.config}
                           typeDefinition={this.props.typeDefinition}/>
        <ConfigurationForm ref="editForm"
                           key="configuration-form-decorator"
                           configFields={this.props.typeDefinition.requested_configuration}
                           title={`Edit ${this.props.typeDefinition.name}`}
                           typeName={decorator.type}
                           includeTitleField={false}
                           submitAction={this._handleSubmit}
                           cancelAction={this._handleCancel}
                           values={decorator.config} />
      </span>
    );
  },
});

export default Decorator;
