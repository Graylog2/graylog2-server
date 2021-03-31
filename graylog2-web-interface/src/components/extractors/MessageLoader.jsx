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

import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';

import { Button } from '../graylog';

const MessagesActions = ActionsProvider.getActions('Messages');
// eslint-disable-next-line no-unused-vars
const MessagesStore = StoreProvider.getStore('Messages');

class MessageLoader extends React.Component {
  static propTypes = {
    hidden: PropTypes.bool,
    hideText: PropTypes.bool,
    onMessageLoaded: PropTypes.func,
  };

  static defaultProps = {
    hidden: true,
  };

  state = {
    hidden: this.props.hidden,
    loading: false,
  };

  toggleMessageForm = () => {
    this.setState({ hidden: !this.state.hidden }, this._focusMessageLoaderForm);
  };

  _focusMessageLoaderForm = () => {
    if (!this.state.hidden) {
      this.messageId.focus();
    }
  };

  loadMessage = (event) => {
    const messageId = this.messageId.value;
    const index = this.index.value;

    if (messageId === '' || index === '') {
      return;
    }

    this.setState({ loading: true });
    const promise = MessagesActions.loadMessage.triggerPromise(index, messageId);

    promise.then((data) => this.props.onMessageLoaded(data));
    promise.finally(() => this.setState({ loading: false }));

    event.preventDefault();
  };

  submit = (messageId, index) => {
    this.messageId.value = messageId;
    this.index.value = index;
    this.submitButton.click();
  };

  render() {
    let explanatoryText;

    if (!this.props.hideText) {
      explanatoryText = (
        <p>
          Wrong example? You can{' '}
          <Button bsStyle="link" bsSize="sm" onClick={this.toggleMessageForm}>load another message</Button>.
        </p>
      );
    }

    const loadMessageForm = (
      <div>
        <form className="form-inline message-loader-form" onSubmit={this.loadMessage}>
          <input type="text" ref={(messageId) => { this.messageId = messageId; }} className="form-control message-id-input" placeholder="Message ID" required />
          <input type="text" ref={(index) => { this.index = index; }} className="form-control" placeholder="Index" required />
          <Button bsStyle="info"
                  ref={(submitButton) => { this.submitButton = submitButton; }}
                  disabled={this.state.loading}
                  type="submit">
            {this.state.loading ? 'Loading message...' : 'Load message'}
          </Button>
        </form>
      </div>
    );

    return (
      <div className="message-loader">
        {explanatoryText}
        {this.state.hidden ? null : loadMessageForm}
      </div>
    );
  }
}

export default MessageLoader;
