/** @jsx React.DOM */

'use strict';

jest.dontMock('../UserPreferencesButton');

describe('UserPreferencesButton', function () {
    it('should load user data when user clicks edit button', function () {
        var React = require('react/addons');
        var ReactTestUtils = React.addons.TestUtils;
        var UserPreferencesButton = require('../UserPreferencesButton');
        var PreferencesStore = require('../../stores/PreferencesStore'); // mocked
        var userName = "Full";

        var instance = ReactTestUtils.renderIntoDocument(
                <UserPreferencesButton userName={userName} />
        );
        var input = ReactTestUtils.findRenderedDOMComponentWithTag(instance, "button");

        ReactTestUtils.Simulate.click(input.getDOMNode());
        expect(PreferencesStore.loadUserPreferences).toBeCalledWith(userName);

    });
});