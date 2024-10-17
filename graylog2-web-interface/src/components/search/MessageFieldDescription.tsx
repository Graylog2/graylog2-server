import React from 'react';
import Immutable from 'immutable';
import styled, { css } from 'styled-components';

import { Alert } from 'components/bootstrap';
import { FULL_MESSAGE_FIELD, MESSAGE_FIELD } from 'views/Constants';

const MessageTerms = styled.span(({ theme }) => css`
  margin-right: 8px;
  font-family: ${theme.fonts.family.monospace};
`);

type MessageFieldDescriptionProps = {
  message: any;
  fieldName: string;
  renderForDisplay: (name: string) => React.ReactNode;
  customFieldActions?: React.ReactElement;
};

class MessageFieldDescription extends React.Component<MessageFieldDescriptionProps, {
  [key: string]: any;
}> {
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
