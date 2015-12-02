import PreferencesStore from 'stores/users/PreferencesStore';
import UserPreferencesButton from 'components/users/UserPreferencesButton';
import UserPreferencesModal from 'components/users/UserPreferencesModal';
import React from 'react';
import ReactTestUtils from 'react-addons-test-utils';
import $ from 'jquery';

describe('UserPreferencesButton', function () {
    it('should load user data when user clicks edit button', function () {
        spyOn(PreferencesStore, 'loadUserPreferences').and.callFake(function (userName, callback) {
            callback([]);
        });

        // we can not mock this, as the plugin is never loaded (we just have jquery)
        $.fn.modal = function () {
        };

        var userName = 'Full';
        var instance = ReactTestUtils.renderIntoDocument(
            <UserPreferencesButton userName={userName}/>
        );
        var input = ReactTestUtils.findRenderedDOMComponentWithTag(instance, 'button');

        ReactTestUtils.Simulate.click(input);
        expect(PreferencesStore.loadUserPreferences).toHaveBeenCalledWith(userName, jasmine.any(Function));
    });
});
