'use strict';

var PreferencesStore = require('../../src/stores/users/PreferencesStore');
var UserPreferencesButton = require('../../src/components/users/UserPreferencesButton');
var UserPreferencesModal = require('../../src/components/users/UserPreferencesModal');
var React = require('react/addons');
var ReactTestUtils = React.addons.TestUtils;
var $ = require('jquery');

describe('UserPreferencesButton', function () {
    it('should load user data when user clicks edit button', function () {
        spyOn(PreferencesStore, "loadUserPreferences").and.callFake(function (userName, callback) {
            callback([]);
        });

        // we can not mock this, as the plugin is never loaded (we just have jquery)
        $.fn.modal = function () {
        }

        var userName = "Full";
        var modal = ReactTestUtils.renderIntoDocument(
            <UserPreferencesModal userName={userName} />
        );
        spyOn(modal, "openModal").and.callThrough();

        var instance = ReactTestUtils.renderIntoDocument(
            <UserPreferencesButton modal={modal}/>
        );
        var input = ReactTestUtils.findRenderedDOMComponentWithTag(instance, "button");

        ReactTestUtils.Simulate.click(input.getDOMNode());
        expect(modal.openModal).toHaveBeenCalled();
        expect(PreferencesStore.loadUserPreferences).toHaveBeenCalledWith(userName, jasmine.any(Function));
    });
});