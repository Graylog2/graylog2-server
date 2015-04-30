'use strict';

var React = require('react/addons');

var UserPreferencesButton = React.createClass({
    render() {
        return (
            <button onClick={this.props.modal.openModal} className="btn btn-success">User preferences</button>
        );
    }
});

module.exports = UserPreferencesButton;
