'use strict';

var React = require('react/addons');
var Button = require('react-bootstrap').Button;
var StreamForm = require('./StreamForm');
var Col = require('react-bootstrap').Col;

var CreateStreamButton = React.createClass({
    onClick(evt) {
        this.refs.streamForm.open();
    },
    render() {
        return (
            <Col md={2} style={{textAlign: 'center', marginTop: '35px'}}>
                <Button bsSize='large' bsStyle='success' onClick={this.onClick}>Create Stream</Button>
                <StreamForm ref='streamForm' title='Creating Stream' onSubmit={this.props.onSave} />
            </Col>
        );
    }
});

module.exports = CreateStreamButton;
