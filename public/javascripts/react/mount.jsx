/** @jsx React.DOM */

$(document).ready(function () {
    var editUserPreferencesButton = document.getElementById('react-user-preferences-button');
    if (editUserPreferencesButton) {
        var userName = editUserPreferencesButton.getAttribute('data-user-name');
        React.renderComponent(<UserPreferencesButton user={userName} />, editUserPreferencesButton);
    }

    var editUserPreferences = document.getElementById('react-user-preferences-modal');
    if (editUserPreferences) React.renderComponent(<UserPreferencesModal />, editUserPreferences);
});