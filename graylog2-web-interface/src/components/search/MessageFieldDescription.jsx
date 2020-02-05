import PropTypes from 'prop-types';
import React from 'react';
import { Alert } from 'components/graylog';
import Immutable from 'immutable';
import styled from 'styled-components';


import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import { DecoratedMessageFieldMarker } from 'components/search';
import DecorationStats from 'logic/message/DecorationStats';
import MessageFieldSearchActions from './MessageFieldSearchActions';

const SearchStore = StoreProvider.getStore('Search');
// eslint-disable-next-line no-unused-vars
const MessagesStore = StoreProvider.getStore('Messages');
const MessagesActions = ActionsProvider.getActions('Messages');

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
    disableFieldActions: PropTypes.bool,
    customFieldActions: PropTypes.node,
  };

  state = {
    messageTerms: Immutable.List(),
  };

  loadTerms = (field) => {
    return () => {
      const promise = MessagesActions.fieldTerms.triggerPromise(this.props.message.index, this.props.message.fields[field]);
      promise.then(terms => this._onTermsLoaded(terms));
    };
  };

  _onTermsLoaded = (terms) => {
    this.setState({ messageTerms: Immutable.fromJS(terms) });
  };

  _shouldShowTerms = () => {
    return this.state.messageTerms.size !== 0;
  };

  addFieldToSearchBar = (event) => {
    event.preventDefault();
    SearchStore.addSearchTerm(this.props.fieldName, this.props.fieldValue);
  };

  _getFormattedTerms = () => {
    const termsMarkup = [];
    this.state.messageTerms.forEach((term, idx) => {
      termsMarkup.push(<MessageTerms key={idx}>{term}</MessageTerms>);
    });

    return termsMarkup;
  };

  _getFormattedFieldActions = () => {
    if (this.props.disableFieldActions) {
      return null;
    }

    let fieldActions;
    if (this.props.customFieldActions) {
      fieldActions = React.cloneElement(this.props.customFieldActions, { fieldName: this.props.fieldName, message: this.props.message });
    } else {
      fieldActions = (
        <MessageFieldSearchActions fieldName={this.props.fieldName}
                                   message={this.props.message}
                                   onAddFieldToSearchBar={this.addFieldToSearchBar}
                                   onLoadTerms={this.loadTerms} />
      );
    }

    return fieldActions;
  };

  render() {
    const { fieldName } = this.props;
    const className = fieldName === 'message' || fieldName === 'full_message' ? 'message-field' : '';
    const isDecorated = DecorationStats.isFieldDecorated(this.props.message, fieldName);

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
        {isDecorated && <DecoratedMessageFieldMarker />}
      </dd>
    );
  }
}

export default MessageFieldDescription;
