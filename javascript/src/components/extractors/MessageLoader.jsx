'use strict';

var React = require('react');

var MessagesStore = require('../../stores/messages/MessagesStore');

var MessageLoader = React.createClass({
    getInitialState() {
        return ({
            hidden: true
        });
    },
    toggleMessageForm() {
        this.setState({hidden: !this.state.hidden}, this._focusMessageLoaderForm);
    },
    _focusMessageLoaderForm() {
        if (!this.state.hidden) {
            this.refs.messageId.getDOMNode().focus();
        }
    },
    loadMessage(e) {
        var messageId = this.refs.messageId.getDOMNode().value;
        var index = this.refs.index.getDOMNode().value;
        if (messageId === "" || index === "") {
            return;
        }
        var promise = MessagesStore.loadMessage(index, messageId);
        promise.done((d) => this.props.onMessageLoaded(d));

        e.preventDefault();
    },
    render() {
        var loadMessageForm = (
            <div>
                <form className="form-inline message-loader-form" onSubmit={this.loadMessage}>
                    <input type="text" ref="messageId" className="form-control" placeholder="Message ID" required/>
                    <input type="text" ref="index" className="form-control" placeholder="Index" required/>
                    <button type="submit" className="btn btn-info">
                        Load a message
                    </button>
                </form>
                <hr/>
            </div>
        );
        return (
            <div className="message-loader">
                <p>
                    Wrong example? You can <button className="btn btn-link btn-small btn-text" onClick={this.toggleMessageForm}>load another message</button>.
                </p>
                {this.state.hidden ? null : loadMessageForm}
            </div>
        );
    }
});

module.exports = MessageLoader;