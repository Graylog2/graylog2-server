'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var UserPreferencesButton = require('./UserPreferencesButton');
var UserPreferencesModal = require('./UserPreferencesModal');
var UserList = require('./UserList');
var RolesComponent = require('./RolesComponent');
var LdapGroupsComponent = require('./LdapGroupsComponent');

var editUserPreferencesButton = document.getElementById('react-user-preferences-button');
var editUserPreferences = document.getElementById('react-user-preferences-modal');

if (editUserPreferences && editUserPreferencesButton) {
    var userName = editUserPreferencesButton.getAttribute('data-user-name');
    var modal = ReactDOM.render(<UserPreferencesModal userName={userName}/>, editUserPreferences);
    ReactDOM.render(<UserPreferencesButton modal={modal} />, editUserPreferencesButton);
}

var userList = document.getElementById('react-user-list');

if (userList) {
    ReactDOM.render(<UserList currentUsername={userList.getAttribute('data-current-username')}/>, userList);
}

var roleList = document.getElementById('react-roles-component');

if (roleList) {
    ReactDOM.render(<RolesComponent />, roleList);
}

var ldapgroups = document.getElementById('react-ldapgroups-component');

if (ldapgroups) {
    ReactDOM.render(<LdapGroupsComponent />, ldapgroups);
}

