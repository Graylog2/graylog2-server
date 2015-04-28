'use strict';

var React = require('react/addons');
var Button = require('react-bootstrap').Button;
var StreamForm = require('./StreamForm');

var CreateStreamButton = React.createClass({
    onClick(evt) {
        this.refs.streamForm.open();
    },
    render() {
        return (
            <div className="col-md-2" style={{textAlign: 'center', marginTop: '35px'}}>
                <Button bsSize='large' bsStyle='success' onClick={this.onClick}>Create Stream</Button>
                <StreamForm ref='streamForm' title='Creating Stream' onSubmit={this.props.onSave} />
            </div>
        );
    }
});

module.exports = CreateStreamButton;
