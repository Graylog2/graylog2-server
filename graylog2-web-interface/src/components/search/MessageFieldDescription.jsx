import React, { PropTypes } from 'react';
import { Alert } from 'react-bootstrap';
import Immutable from 'immutable';

import StoreProvider from 'injection/StoreProvider';
const SearchStore = StoreProvider.getStore('Search');
// eslint-disable-next-line no-unused-vars
const MessagesStore = StoreProvider.getStore('Messages');

import ActionsProvider from 'injection/ActionsProvider';
const MessagesActions = ActionsProvider.getActions('Messages');

import MessageFieldSearchActions from './MessageFieldSearchActions';

import { DecoratedMessageFieldMarker } from 'components/search';

const MessageFieldDescription = React.createClass({
  propTypes: {
    message: PropTypes.object.isRequired,
    fieldName: PropTypes.string.isRequired,
    fieldValue: PropTypes.any.isRequired,
    possiblyHighlight: PropTypes.func.isRequired,
    disableFieldActions: PropTypes.bool,
    customFieldActions: PropTypes.node,
    isDecorated: PropTypes.bool,
  },
  getInitialState() {
    return {
      messageTerms: Immutable.List(),
    };
  },
  loadTerms(field) {
    return () => {
      const promise = MessagesActions.fieldTerms.triggerPromise(this.props.message.index, this.props.message.fields[field]);
      promise.then(terms => this._onTermsLoaded(terms));
    };
  },
  _onTermsLoaded(terms) {
    this.setState({ messageTerms: Immutable.fromJS(terms) });
  },
  _shouldShowTerms() {
    return this.state.messageTerms.size !== 0;
  },
  addFieldToSearchBar(event) {
    event.preventDefault();
    SearchStore.addSearchTerm(this.props.fieldName, this.props.fieldValue);
  },
  _getFormattedTerms() {
    const termsMarkup = [];
    this.state.messageTerms.forEach((term, idx) => {
      termsMarkup.push(<span key={idx} className="message-terms">{term}</span>);
    });

    return termsMarkup;
  },
  _getFormattedFieldActions() {
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
  },
  render() {
    const className = this.props.fieldName === 'message' || this.props.fieldName === 'full_message' ? 'message-field' : '';

    return (
      <dd className={className} key={`${this.props.fieldName}dd`}>
        {this._getFormattedFieldActions()}
        <div className="field-value">{this.props.possiblyHighlight(this.props.fieldName)}</div>
        {this._shouldShowTerms() &&
        <Alert bsStyle="info" onDismiss={() => this.setState({ messageTerms: Immutable.Map() })}>
          Field terms: &nbsp;{this._getFormattedTerms()}
        </Alert>
        }
        {this.props.isDecorated && <DecoratedMessageFieldMarker />}
      </dd>
    );
  },
});

export default MessageFieldDescription;
