import React from 'react';
import Reflux from 'reflux';

import { Button, Col, Row } from 'react-bootstrap';

import { Spinner } from 'components/common';
import { ConfigurationForm, ConfigurationWell } from 'components/configurationforms';

import StoreProvider from 'injection/StoreProvider';
const DecoratorsStore = StoreProvider.getStore('Decorators');

import ActionsProvider from 'injection/ActionsProvider';
const DecoratorsActions = ActionsProvider.getActions('Decorators');

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
  render() {
    if (!this.state.types) {
      return <Spinner />;
    }
    const decorator = this.props.decorator;
    const decoratorType = this.state.types[decorator.type];
    return (
      <Row className="row-sm">
        <Col md={8}>
          <strong>{decoratorType.name}</strong>
        </Col>
        <Col md={4} className="text-right">
          <Button bsStyle="primary" bsSize="xsmall" onClick={this._handleDeleteClick}>Delete</Button>
          {' '}
          <Button bsStyle="info" bsSize="xsmall" onClick={this._handleEditClick}>Edit</Button>
        </Col>
        <Col md={12}>
          <ConfigurationWell key={`configuration-well-decorator-${decorator._id}`}
                             id={decorator._id}
                             configuration={decorator.config}
                             typeDefinition={this.props.typeDefinition}/>
        </Col>
        <ConfigurationForm ref="editForm"
                           key="configuration-form-decorator"
                           configFields={this.props.typeDefinition.requested_configuration}
                           title={`Edit ${this.props.typeDefinition.name}`}
                           typeName={decorator.type}
                           includeTitleField={false}
                           submitAction={this._handleSubmit}
                           cancelAction={this._handleCancel}
                           values={decorator.config} />
      </Row>
    );
  },
});

export default Decorator;
