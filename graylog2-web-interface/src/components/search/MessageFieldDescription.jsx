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
import Immutable from 'immutable';
import styled, { css } from 'styled-components';

import { Alert } from 'components/graylog';
import { FULL_MESSAGE_FIELD, MESSAGE_FIELD } from 'views/Constants';

const MessageTerms = styled.span(({ theme }) => css`
  margin-right: 8px;
  font-family: ${theme.fonts.family.monospace};
`);

class MessageFieldDescription extends React.Component {
  static propTypes = {
    message: PropTypes.object.isRequired,
    fieldName: PropTypes.string.isRequired,
    renderForDisplay: PropTypes.func.isRequired,
    customFieldActions: PropTypes.node,
  };

  static defaultProps = {
    customFieldActions: undefined,
  };

  constructor(props) {
    super(props);

    this.state = {
      messageTerms: Immutable.List(),
    };
  }

  _shouldShowTerms = () => {
    const { messageTerms } = this.state;

    return messageTerms.size !== 0;
  };

  _getFormattedTerms = () => {
    const { messageTerms } = this.state;

    return messageTerms.map((term) => <MessageTerms key={term}>{term}</MessageTerms>);
  };

  _getFormattedFieldActions = () => {
    const { customFieldActions, fieldName, message } = this.props;

    return customFieldActions ? React.cloneElement(customFieldActions, { fieldName, message }) : null;
  };

  render() {
    const { fieldName, renderForDisplay } = this.props;
    const className = fieldName === MESSAGE_FIELD || fieldName === FULL_MESSAGE_FIELD ? 'message-field' : '';

    return (
      <dd className={className} key={`${fieldName}dd`}>
        {this._getFormattedFieldActions()}
        <div className="field-value">{renderForDisplay(fieldName)}</div>
        {this._shouldShowTerms()
        && (
        <Alert bsStyle="info" onDismiss={() => this.setState({ messageTerms: Immutable.Map() })}>
          Field terms: &nbsp;{this._getFormattedTerms()}
        </Alert>
        )}
      </dd>
    );
  }
}

export default MessageFieldDescription;
