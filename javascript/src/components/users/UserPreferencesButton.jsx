'use strict';

var React = require('react/addons');

var UserPreferencesButton = React.createClass({
    render() {
        return (
            <button onClick={this.props.modal.openModal} className="btn btn-primary btn-small">Edit user preferences</button>
        );
    }
});

module.exports = UserPreferencesButton;
