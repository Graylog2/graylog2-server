'use strict';

var React = require('React');
var PreferencesStore = require('../stores/PreferencesStore');

var UserPreferencesButton = React.createClass({
    _onClick: function (event) {
        PreferencesStore.loadUserPreferences(this.props.userName);
    },
    render: function () {
        return (
            <button onClick={this._onClick} className="btn btn-primary btn-small">Edit user preferences</button>
        );
    }
});

module.exports = UserPreferencesButton;