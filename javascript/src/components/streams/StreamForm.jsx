'use strict';

var React = require('react/addons');
var BootstrapModal = require('../bootstrap/BootstrapModal');
var Input = require('react-bootstrap').Input;

var StreamForm = React.createClass({
    mixins: [React.addons.LinkedStateMixin],
    getInitialState() {
        return this._getValuesFromProps(this.props);
    },
    getDefaultProps() {
        return {
            stream: {title: "", description: ""}
        };
    },
    _resetValues() {
        this.setState(this._getValuesFromProps(this.props));
    },
    _getValuesFromProps(props) {
        return {
            title: props.stream.title,
            description: props.stream.description
        };
    },
    _onSubmit(evt) {
        this.props.onSubmit(this.props.stream.id, {title: this.state.title, description: this.state.description});
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
            <BootstrapModal ref='modal' onCancel={this.close} onConfirm={this._onSubmit} cancel="Cancel" confirm="Save">
                <div>
                    <h2>{this.props.title}</h2>
                </div>
                <div>
                    <Input type='text' required={true} label='Title' placeholder='A descriptive name of the new stream' valueLink={this.linkState('title')}/>
                    <Input type='text' required={true} label='Description' placeholder='What kind of messages are routed into this stream?' valueLink={this.linkState('description')}/>
                </div>
            </BootstrapModal>
        );
    }
});

module.exports = StreamForm;
