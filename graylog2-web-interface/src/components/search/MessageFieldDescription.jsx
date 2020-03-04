import PropTypes from 'prop-types';
import React from 'react';
import { Alert } from 'components/graylog';
import Immutable from 'immutable';
import styled from 'styled-components';

const MessageTerms = styled.span`
  margin-right: 8px;
  font-family: monospace;
`;

class MessageFieldDescription extends React.Component {
  static propTypes = {
    message: PropTypes.object.isRequired,
    fieldName: PropTypes.string.isRequired,
    fieldValue: PropTypes.any.isRequired,
    renderForDisplay: PropTypes.func.isRequired,
    customFieldActions: PropTypes.node,
  };

  static defaultProps = {
    customFieldActions: undefined,
  };

  state = {
    messageTerms: Immutable.List(),
  };

  _shouldShowTerms = () => {
    return this.state.messageTerms.size !== 0;
  };

  _getFormattedTerms = () => {
    const termsMarkup = [];
    this.state.messageTerms.forEach((term, idx) => {
      termsMarkup.push(<MessageTerms key={idx}>{term}</MessageTerms>);
    });

    return termsMarkup;
  };

  _getFormattedFieldActions = () => {
    const { customFieldActions, fieldName, message } = this.props;
    return customFieldActions ? React.cloneElement(customFieldActions, { fieldName, message }) : null;
  };

  render() {
    const { fieldName } = this.props;
    const className = fieldName === 'message' || fieldName === 'full_message' ? 'message-field' : '';

    return (
      <dd className={className} key={`${fieldName}dd`}>
        {this._getFormattedFieldActions()}
        <div className="field-value">{this.props.renderForDisplay(this.props.fieldName)}</div>
        {this._shouldShowTerms()
        && (
        <Alert bsStyle="info" onDismiss={() => this.setState({ messageTerms: Immutable.Map() })}>
          Field terms: &nbsp;{this._getFormattedTerms()}
        </Alert>
        )
        }
      </dd>
    );
  }
}

export default MessageFieldDescription;
