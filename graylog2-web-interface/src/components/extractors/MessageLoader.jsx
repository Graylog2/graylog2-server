import React, {PropTypes} from 'react';

import ActionsProvider from 'injection/ActionsProvider';
const MessagesActions = ActionsProvider.getActions('Messages');

import StoreProvider from 'injection/StoreProvider';
// eslint-disable-next-line no-unused-vars
const MessagesStore = StoreProvider.getStore('Messages');

const MessageLoader = React.createClass({
  propTypes: {
    hidden: PropTypes.bool,
    hideText: PropTypes.bool,
    onMessageLoaded: PropTypes.func,
  },
  getDefaultProps() {
    return {
      hidden: true,
    };
  },
  getInitialState() {
    return ({
      hidden: this.props.hidden,
      loading: false,
    });
  },

  toggleMessageForm() {
    this.setState({hidden: !this.state.hidden}, this._focusMessageLoaderForm);
  },
  _focusMessageLoaderForm() {
    if (!this.state.hidden) {
      this.refs.messageId.focus();
    }
  },
  loadMessage(event) {
    const messageId = this.refs.messageId.value;
    const index = this.refs.index.value;
    if (messageId === '' || index === '') {
      return;
    }
    this.setState({ loading: true });
    const promise = MessagesActions.loadMessage.triggerPromise(index, messageId);
    promise.then(data => this.props.onMessageLoaded(data));
    promise.finally(() => this.setState({ loading: false }));

    event.preventDefault();
  },
  submit(messageId, index) {
    this.refs.messageId.value = messageId;
    this.refs.index.value = index;
    this.refs.submitButton.click();
  },
  render() {
    let explanatoryText;
    if (!this.props.hideText) {
      explanatoryText = (
        <p>
          Wrong example? You can{' '}
          <button className="btn btn-link btn-small btn-text" onClick={this.toggleMessageForm}>load another message</button>.
        </p>
      );
    }
    const loadMessageForm = (
      <div>
        <form className="form-inline message-loader-form" onSubmit={this.loadMessage}>
          <input type="text" ref="messageId" className="form-control" placeholder="Message ID" required/>
          <input type="text" ref="index" className="form-control" placeholder="Index" required/>
          <button ref="submitButton" type="submit" className="btn btn-info" disabled={this.state.loading}>
            {this.state.loading ? 'Loading message...' : 'Load message'}
          </button>
        </form>
      </div>
    );
    return (
      <div className="message-loader">
        {explanatoryText}
        {this.state.hidden ? null : loadMessageForm}
      </div>
    );
  },
});

export default MessageLoader;
