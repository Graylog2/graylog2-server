/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';

import { Col, Row } from 'components/graylog';
import { Pluralize } from 'components/common';

const SimulationChangesWrap = styled.div`
  padding-top: 15px;

  dl {
    margin-bottom: 10px;
    margin-top: 5px;
  }

  dd {
    padding: 1px 9px 3px;
  }

  dt {
    margin-top: 1px;
    padding: 3px 9px 1px;

    &::after {
      content: ": ";
    }

    &:first-child {
      border-radius: 4px 4px 0 0;
    }

    ~ dd:last-child {
      border-radius: 0 0 4px 4px;
    }
  }
`;

const OriginalChanges = styled.div`
  margin-top: 10px;
`;

const FieldResultWrap = styled.div(({ resultType, theme }) => {
  const { success, danger, info } = theme.colors.variant.light;
  const types = {
    added: success,
    removed: danger,
    mutated: info,
  };

  return `
    dt,
    dd {
      background-color: ${types[resultType]};
      color: ${theme.utils.contrastingColor(types[resultType])};
    }
  `;
});

const FieldValue = styled.dd(({ removed, theme }) => css`
  font-family: ${theme.fonts.family.monospace};

  ${removed && css`
    text-decoration: line-through;
    font-style: italic;
  `}
`);

const SimulationChanges = createReactClass({
  displayName: 'SimulationChanges',

  propTypes: {
    originalMessage: PropTypes.object.isRequired,
    simulationResults: PropTypes.object.isRequired,
  },

  _isOriginalMessageRemoved(originalMessage, processedMessages) {
    return !processedMessages.find((message) => message.id === originalMessage.id);
  },

  _formatFieldTitle(field) {
    return <dt key={`${field}-key`}>{field}</dt>;
  },

  _formatFieldValue(field, value, isRemoved = false) {
    return <FieldValue key={`${field}-value`} removed={isRemoved}>{String(value)}</FieldValue>;
  },

  _formatAddedFields(originalMessage, processedMessage) {
    const originalFields = Object.keys(originalMessage.fields);
    const processedFields = Object.keys(processedMessage.fields);

    const addedFields = processedFields.filter((field) => originalFields.indexOf(field) === -1);

    if (addedFields.length === 0) {
      return null;
    }

    const formattedFields = [];

    addedFields.sort().forEach((field) => {
      formattedFields.push(this._formatFieldTitle(field));
      formattedFields.push(this._formatFieldValue(field, processedMessage.fields[field]));
    });

    return (
      <FieldResultWrap resultType="added">
        <h4>Added fields</h4>
        <dl>
          {formattedFields}
        </dl>
      </FieldResultWrap>
    );
  },

  _formatRemovedFields(originalMessage, processedMessage) {
    const originalFields = Object.keys(originalMessage.fields);
    const processedFields = Object.keys(processedMessage.fields);

    const removedFields = originalFields.filter((field) => processedFields.indexOf(field) === -1);

    if (removedFields.length === 0) {
      return null;
    }

    const formattedFields = [];

    removedFields.sort().forEach((field) => {
      formattedFields.push(this._formatFieldTitle(field));
      formattedFields.push(this._formatFieldValue(field, originalMessage.fields[field]));
    });

    return (
      <FieldResultWrap resultType="removed">
        <h4>Removed fields</h4>
        <dl>
          {formattedFields}
        </dl>
      </FieldResultWrap>
    );
  },

  _formatMutatedFields(originalMessage, processedMessage) {
    const originalFields = Object.keys(originalMessage.fields);
    const processedFields = Object.keys(processedMessage.fields);

    const mutatedFields = [];

    originalFields.forEach((field) => {
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

    mutatedFields.sort().forEach((field) => {
      formattedFields.push(this._formatFieldTitle(field));
      formattedFields.push(this._formatFieldValue(`${field}-original`, originalMessage.fields[field], true));
      formattedFields.push(this._formatFieldValue(field, processedMessage.fields[field]));
    });

    return (
      <FieldResultWrap resultType="mutated">
        <h4>Mutated fields</h4>
        <dl>
          {formattedFields}
        </dl>
      </FieldResultWrap>
    );
  },

  _getOriginalMessageChanges() {
    const { originalMessage, simulationResults } = this.props;
    const processedMessages = simulationResults.messages;

    if (this._isOriginalMessageRemoved(originalMessage, processedMessages)) {
      return <p>Original message would be dropped during processing.</p>;
    }

    const processedMessage = processedMessages.find((message) => message.id === originalMessage.id);

    const formattedAddedFields = this._formatAddedFields(originalMessage, processedMessage);
    const formattedRemovedFields = this._formatRemovedFields(originalMessage, processedMessage);
    const formattedMutatedFields = this._formatMutatedFields(originalMessage, processedMessage);

    if (!formattedAddedFields && !formattedRemovedFields && !formattedMutatedFields) {
      return <p>Original message would be not be modified during processing.</p>;
    }

    return (
      <OriginalChanges>
        {formattedAddedFields}
        {formattedRemovedFields}
        {formattedMutatedFields}
      </OriginalChanges>
    );
  },

  _formatOriginalMessageChanges() {
    const { originalMessage } = this.props;

    return (
      <Row className="row-sm">
        <Col md={12}>
          <h3>
            Changes in original message{' '}
            <small><em>{originalMessage.id}</em></small>
          </h3>
          {this._getOriginalMessageChanges()}
        </Col>
      </Row>
    );
  },

  _formatOtherChanges() {
    const { originalMessage, simulationResults } = this.props;

    const createdMessages = simulationResults.messages.filter((message) => message.id !== originalMessage.id);

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
      <SimulationChangesWrap>
        {this._formatOriginalMessageChanges()}
        {this._formatOtherChanges()}
      </SimulationChangesWrap>
    );
  },
});

export default SimulationChanges;
