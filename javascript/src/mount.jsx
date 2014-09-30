'use strict';

var React = require('react/addons');
var UserPreferencesButton = require('./components/UserPreferencesButton');
var UserPreferencesModal = require('./components/UserPreferencesModal');
var CardList = require('./components/CardList');
var ConfigurationBundles = require('./components/ConfigurationBundles');
var $ = require('jquery'); // excluded and shimed

$(document).ready(function () {
    var editUserPreferencesButton = document.getElementById('react-user-preferences-button');
    if (editUserPreferencesButton) {
        var userName = editUserPreferencesButton.getAttribute('data-user-name');
        React.renderComponent(<UserPreferencesButton userName={userName} />, editUserPreferencesButton);
    }

    var editUserPreferences = document.getElementById('react-user-preferences-modal');
    if (editUserPreferences) {
        React.renderComponent(<UserPreferencesModal />, editUserPreferences);
    }

    var cardList = document.getElementById('react-card-list');
    if (cardList) {
        React.renderComponent(<CardList />, cardList);
    }

    var configurationBundles = document.getElementById('react-configuration-bundles');
    if (configurationBundles) {
        React.renderComponent(<ConfigurationBundles />, configurationBundles);
    }
});
