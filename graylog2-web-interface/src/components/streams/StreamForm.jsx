'use strict';

var React = require('react');
var LinkedStateMixin = require('react-addons-linked-state-mixin');
var BootstrapModalForm = require('../bootstrap/BootstrapModalForm');
var Input = require('react-bootstrap').Input;

var StreamForm = React.createClass({
    propTypes: {
      onSubmit: React.PropTypes.func.isRequired,
    },
    mixins: [LinkedStateMixin],
    getInitialState() {
        return this._getValuesFromProps(this.props);
    },
    getDefaultProps() {
        return {
            stream: {
                title: "",
                description: "",
                remove_from_all_messages: false
            }
        };
    },
    _resetValues() {
        this.setState(this._getValuesFromProps(this.props));
    },
    _getValuesFromProps(props) {
        return {
            title: props.stream.title,
            description: props.stream.description,
            remove_from_all_messages: props.stream.remove_from_all_messages
        };
    },
    _onSubmit(evt) {
        this.props.onSubmit(this.props.stream.id,
            {
                title: this.state.title,
                description: this.state.description,
                remove_from_all_messages: this.state.remove_from_all_messages,
            });
        this.refs.modal.close();
    },
    open() {
        this._resetValues();
        this.refs.modal.open();
    },
    close() {
        this.refs.modal.close();
    },
    render() {
        return (
            <BootstrapModalForm ref="modal"
                                title={this.props.title}
                                onSubmitForm={this._onSubmit}
                                submitButtonText="Save">
                <Input type='text' required={true} label='Title' placeholder='A descriptive name of the new stream' valueLink={this.linkState('title')} autoFocus />
                <Input type='text' required={true} label='Description' placeholder='What kind of messages are routed into this stream?' valueLink={this.linkState('description')}/>
                <Input type="checkbox" label="Remove from All Messages stream" checkedLink={this.linkState('remove_from_all_messages')}/>
            </BootstrapModalForm>
        );
    }
});

module.exports = StreamForm;
