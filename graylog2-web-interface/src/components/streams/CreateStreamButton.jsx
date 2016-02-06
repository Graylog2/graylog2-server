'use strict';

var React = require('react');
var Button = require('react-bootstrap').Button;
var StreamForm = require('./StreamForm');

var CreateStreamButton = React.createClass({
    propTypes: {
      onSave: React.PropTypes.func.isRequired,
    },
    onClick() {
        this.refs.streamForm.open();
    },
    render() {
        return (
            <span>
                <Button bsSize={this.props.bsSize} bsStyle={this.props.bsStyle} className={this.props.className} onClick={this.onClick}>
                    {this.props.buttonText || "Create Stream"}
                </Button>
                <StreamForm ref='streamForm' title='Creating Stream' onSubmit={this.props.onSave} />
            </span>
        );
    }
});

module.exports = CreateStreamButton;
