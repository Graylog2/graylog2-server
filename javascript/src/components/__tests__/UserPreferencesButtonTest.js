'use strict';

jest.dontMock('../users/UserPreferencesButton');
jest.dontMock('../users/UserPreferencesModal');

// this test is of questionable value, it is not included in the build
// it is left here as an example how to write a UI test if you have a scenario that can benefit from a test
describe('UserPreferencesButton', function () {
    it('should load user data when user clicks edit button', function () {
        var React = require('react/addons');
        var ReactTestUtils = React.addons.TestUtils;
        var UserPreferencesButton = require('../users/UserPreferencesButton');
        var UserPreferencesModal = require('../users/UserPreferencesModal');
        var $ = require('jquery');
        $.mockReturnValue({
            modal: function () {
            }
        });

        var userName = "Full";
        var modal = ReactTestUtils.renderIntoDocument(
            <UserPreferencesModal userName={userName} />
        );
        var openModal = jest.genMockFunction();
        modal.openModal = openModal;

        var instance = ReactTestUtils.renderIntoDocument(
            <UserPreferencesButton modal={modal}/>
        );
        var input = ReactTestUtils.findRenderedDOMComponentWithTag(instance, "button");

        ReactTestUtils.Simulate.click(input.getDOMNode());
        expect(modal.openModal).toBeCalled();
    });
});