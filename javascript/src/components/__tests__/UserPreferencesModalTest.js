/** @jsx React.DOM */

'use strict';

jest.dontMock('../../lib/util');
jest.dontMock('../UserPreferencesButton');
jest.dontMock('../UserPreferencesModal');

describe('UserPreferencesModal', function () {
    it('should properly register to store upon creation', function () {
        var React = require('react/addons');
        var ReactTestUtils = React.addons.TestUtils;
        var UserPreferencesButton = require('../UserPreferencesButton');
        var UserPreferencesModal = require('../UserPreferencesModal');
        var PreferencesStore = require('../../stores/PreferencesStore'); // mocked
        var $ = require('jquery');
        $.mockReturnValue({
            modal: function () {
            }
        });
        var userName = "Full";

        var instance = ReactTestUtils.renderIntoDocument(
            <div>
                <UserPreferencesButton userName={userName} />
                <UserPreferencesModal />
            </div>
        );
        expect(PreferencesStore.addChangeListener).toBeCalledWith(UserPreferencesModal._onInputChanged);
        expect(PreferencesStore.on).toBeCalledWith(PreferencesStore.DATA_LOADED_EVENT, PreferencesStore._openModal);
        expect(PreferencesStore.on).toBeCalledWith(PreferencesStore.DATA_SAVED_EVENT, this._closeModal);
    });
});