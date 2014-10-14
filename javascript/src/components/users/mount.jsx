'use strict';

var React = require('react/addons');
var UserPreferencesButton = require('./UserPreferencesButton');
var UserPreferencesModal = require('./UserPreferencesModal');

var editUserPreferencesButton = document.getElementById('react-user-preferences-button');
if (editUserPreferencesButton) {
    var userName = editUserPreferencesButton.getAttribute('data-user-name');
    React.renderComponent(<UserPreferencesButton userName={userName} />, editUserPreferencesButton);
}

var editUserPreferences = document.getElementById('react-user-preferences-modal');
if (editUserPreferences) {
    React.renderComponent(<UserPreferencesModal />, editUserPreferences);
}
