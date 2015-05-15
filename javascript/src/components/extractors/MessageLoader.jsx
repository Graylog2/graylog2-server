'use strict';

var React = require('react');

var MessagesStore = require('../../stores/messages/MessagesStore');

var MessageLoader = React.createClass({
    getInitialState() {
        return ({
            hidden: this.props.hidden
        });
    },
    getDefaultProps() {
        return {
            hidden: true
        };
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
        var messageId = React.findDOMNode(this.refs.messageId).value;
        var index = React.findDOMNode(this.refs.index).value;
        if (messageId === "" || index === "") {
            return;
        }
        var promise = MessagesStore.loadMessage(index, messageId);
        promise.done((d) => this.props.onMessageLoaded(d));

        e.preventDefault();
    },
    submit(messageId, index) {
        React.findDOMNode(this.refs.messageId).value = messageId;
        React.findDOMNode(this.refs.index).value = index;
        React.findDOMNode(this.refs.submitButton).click();
    },
    render() {
        var explanatoryText = (this.props.hideText ? null :
            <p>
                Wrong example? You can <button className="btn btn-link btn-small btn-text" onClick={this.toggleMessageForm}>load another message</button>.
            </p>
        );
        var loadMessageForm = (
            <div>
                <form className="form-inline message-loader-form" onSubmit={this.loadMessage}>
                    <input type="text" ref="messageId" className="input-xlarge" placeholder="Message ID" required/>
                    <input type="text" ref="index" className="input-medium" placeholder="Index" required/>
                    <button ref="submitButton" type="submit" className="btn">
                        Load a message
                    </button>
                </form>
                <hr/>
            </div>
        );
        return (
            <div className="message-loader">
                {explanatoryText}
                {this.state.hidden ? null : loadMessageForm}
            </div>
        );
    }
});

module.exports = MessageLoader;