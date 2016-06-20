import React from 'react';
import Reflux from 'reflux';

import { Button, Col, Row } from 'react-bootstrap';

import { Spinner } from 'components/common';
import { ConfigurationWell } from 'components/configurationforms';

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

  _deleteDecorator() {
    if (window.confirm('Do you really want to delete this decorator?')) {
      DecoratorsActions.remove(this.props.decorator._id);
    }
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
          <Button bsStyle="primary" bsSize="xsmall" onClick={this._deleteDecorator}>Delete</Button>
          {' '}
          <Button bsStyle="info" bsSize="xsmall" onClick={this._deleteDecorator}>Edit</Button>
        </Col>
        <Col md={12}>
          <ConfigurationWell key={"configuration-well-decorator-" + decorator._id}
                             id={decorator._id} configuration={decorator.config}
                             typeDefinition={this.state.typeDefinition}/>
        </Col>
      </Row>
    );
  },
});

export default Decorator;
