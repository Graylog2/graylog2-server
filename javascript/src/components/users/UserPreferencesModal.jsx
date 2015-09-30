'use strict';

var React = require('react');
var PreferencesStore = require('../../stores/users/PreferencesStore');
var BootstrapModalForm = require('../bootstrap/BootstrapModalForm');

var UserPreferencesModal = React.createClass({
    getInitialState() {
        return {preferences: []};
    },
    _onPreferenceChanged(name, event) {
        var preferenceToChange = this.state.preferences.filter((preference) => preference.name === name)[0];
        // TODO: we need the type of the preference to set it properly
        if (preferenceToChange) {
            preferenceToChange.value = event.target.value;
            this.setState({preferences: this.state.preferences});
        }
    },
    render() {
        // TODO: Add additional row where you can add a new preference
        // TODO: Add delete button
        let shouldAutoFocus = true;

        const formattedPreferences = this.state.preferences.map((preference, index) => {
            const formattedPreference = (
              <div className="form-group" key={index}>
                  <label htmlFor={preference.name+"-"+index}>{preference.name}</label>
                  <input id={preference.name+"-"+index}
                         onChange={this._onPreferenceChanged.bind(this, preference.name)}
                         className="form-control"
                         value={preference.value}
                         type="text"
                         required
                         autoFocus={shouldAutoFocus} />
              </div>
            );

            if (shouldAutoFocus) {
                shouldAutoFocus = false;
            }

            return formattedPreference;
        });
        return (
            <BootstrapModalForm ref="modal"
                                title={`Preferences for user ${this.props.userName}`}
                                onSubmitForm={this._save}
                                submitButtonText="Save">
                <div>{formattedPreferences}</div>
            </BootstrapModalForm>
        );
    },
    openModal() {
        PreferencesStore.loadUserPreferences(this.props.userName, (preferences) => {
            this.setState({preferences: preferences});
            this.refs.modal.open();
        });
    },
    _save() {
        PreferencesStore.saveUserPreferences(this.state.preferences, this.refs.modal.close);
    }

});

module.exports = UserPreferencesModal;
