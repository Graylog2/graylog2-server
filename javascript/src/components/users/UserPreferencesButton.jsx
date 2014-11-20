'use strict';

var React = require('react/addons');
var PreferencesStore = require('../../stores/users/PreferencesStore');

var UserPreferencesButton = React.createClass({
    _onClick(event) {
        PreferencesStore.loadUserPreferences(this.props.userName);
    },
    render() {
        return (
            <button onClick={this._onClick} className="btn btn-primary btn-small">Edit user preferences</button>
        );
    }
});

module.exports = UserPreferencesButton;
