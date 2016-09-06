import React from 'react';
import { Col, Row } from 'react-bootstrap';

import { Pluralize } from 'components/common';

const SimulationChanges = React.createClass({
  propTypes: {
    originalMessage: React.PropTypes.object.isRequired,
    simulationResults: React.PropTypes.object.isRequired,
  },

  componentDidMount() {
    this.style.use();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  style: require('!style/useable!css!./SimulationChanges.css'),

  _isOriginalMessageRemoved(originalMessage, processedMessages) {
    return !processedMessages.find(message => message.id === originalMessage.id);
  },

  _formatFieldTitle(field) {
    return <dt key={`${field}-key`}>{field}</dt>;
  },

  _formatFieldValue(field, value, isAdded, isRemoved) {
    const className = (isAdded ? 'added-field' : (isRemoved ? 'removed-field' : ''));
    return <dd key={`${field}-value`} className={`field-value ${className}`}>{String(value)}</dd>;
  },

  _formatAddedFields(originalMessage, processedMessage) {
    const originalFields = Object.keys(originalMessage.fields);
    const processedFields = Object.keys(processedMessage.fields);

    const addedFields = processedFields.filter(field => originalFields.indexOf(field) === -1);

    if (addedFields.length === 0) {
      return null;
    }

    const formattedFields = [];

    addedFields.sort().forEach(field => {
      formattedFields.push(this._formatFieldTitle(field));
      formattedFields.push(this._formatFieldValue(field, processedMessage.fields[field], true, false));
    });

    return (
      <div className="added-fields">
        <h4>Added fields</h4>
        <dl>
          {formattedFields}
        </dl>
      </div>
    );
  },

  _formatRemovedFields(originalMessage, processedMessage) {
    const originalFields = Object.keys(originalMessage.fields);
    const processedFields = Object.keys(processedMessage.fields);

    const removedFields = originalFields.filter(field => processedFields.indexOf(field) === -1);

    if (removedFields.length === 0) {
      return null;
    }

    const formattedFields = [];

    removedFields.sort().forEach(field => {
      formattedFields.push(this._formatFieldTitle(field));
      formattedFields.push(this._formatFieldValue(field, originalMessage.fields[field], false, true));
    });

    return (
      <div className="removed-fields">
        <h4>Removed fields</h4>
        <dl>
          {formattedFields}
        </dl>
      </div>
    );
  },

  _formatMutatedFields(originalMessage, processedMessage) {
    const originalFields = Object.keys(originalMessage.fields);
    const processedFields = Object.keys(processedMessage.fields);

    const mutatedFields = [];

    originalFields.forEach(field => {
      if (processedFields.indexOf(field) === -1) {
        return;
      }
      const originalValue = originalMessage.fields[field];
      const processedValue = processedMessage.fields[field];

      if (typeof originalValue !== typeof processedValue) {
        mutatedFields.push(field);
        return;
      }

      // Convert to JSON to avoid problems comparing objects or arrays. Yes, this sucks :/
      if (JSON.stringify(originalValue) !== JSON.stringify(processedValue)) {
        mutatedFields.push(field);
      }
    });

    if (mutatedFields.length === 0) {
      return null;
    }

    const formattedFields = [];

    mutatedFields.sort().forEach(field => {
      formattedFields.push(this._formatFieldTitle(field));
      formattedFields.push(this._formatFieldValue(`${field}-original`, originalMessage.fields[field], false, true));
      formattedFields.push(this._formatFieldValue(field, processedMessage.fields[field], true, false));
    });

    return (
      <div className="mutated-fields">
        <h4>Mutated fields</h4>
        <dl>
          {formattedFields}
        </dl>
      </div>
    );
  },

  _getOriginalMessageChanges() {
    const originalMessage = this.props.originalMessage;
    const processedMessages = this.props.simulationResults.messages;

    if (this._isOriginalMessageRemoved(originalMessage, processedMessages)) {
      return <p>Original message would be dropped during processing.</p>;
    }

    const processedMessage = processedMessages.find(message => message.id === originalMessage.id);

    const formattedAddedFields = this._formatAddedFields(originalMessage, processedMessage);
    const formattedRemovedFields = this._formatRemovedFields(originalMessage, processedMessage);
    const formattedMutatedFields = this._formatMutatedFields(originalMessage, processedMessage);

    if (!formattedAddedFields && !formattedRemovedFields && !formattedMutatedFields) {
      return <p>Original message would be not be modified during processing.</p>;
    }

    return (
      <div className="original-message-changes">
        {formattedAddedFields}
        {formattedRemovedFields}
        {formattedMutatedFields}
      </div>
    );
  },

  _formatOriginalMessageChanges() {
    return (
      <Row className="row-sm">
        <Col md={12}>
          <h3>
            Changes in original message{' '}
            <small><em>{this.props.originalMessage.id}</em></small>
          </h3>
          {this._getOriginalMessageChanges()}
        </Col>
      </Row>
    );
  },

  _formatOtherChanges() {
    const originalMessageId = this.props.originalMessage.id;
    const simulatedMessages = this.props.simulationResults.messages;

    const createdMessages = simulatedMessages.filter(message => message.id !== originalMessageId);

    if (createdMessages.length === 0) {
      return null;
    }

    return (
      <Row className="row-sm">
        <Col md={12}>
          <h3>Other changes</h3>
          <p>
            There would be {createdMessages.length}{' '}
            <Pluralize singular="message" plural="messages" value={createdMessages.length} /> created.{' '}
            Switch to the <em>Results preview</em> view option to see{' '}
            <Pluralize singular="it" plural="them" value={createdMessages.length} />.
          </p>
        </Col>
      </Row>
    );
  },

  render() {
    return (
      <div className="simulation-changes">
        {this._formatOriginalMessageChanges()}
        {this._formatOtherChanges()}
      </div>
    );
  },
});

export default SimulationChanges;
