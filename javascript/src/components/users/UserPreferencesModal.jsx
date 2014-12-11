'use strict';

var React = require('react/addons');
var PreferencesStore = require('../../stores/users/PreferencesStore');
var BootstrapModal = require('../bootstrap/BootstrapModal');

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
        var header = <h2>Preferences for user {this.props.userName}</h2>;
        // TODO: Add additional row where you can add a new preference
        // TODO: Add delete button
        var body = (
            <div>
            {this.state.preferences.map((preference, index) => {
                return (
                    <div className="control-group" key={index}>
                        <label htmlFor={preference.name+"-"+index} className="control-label">{preference.name}</label>
                        <div className="controls">
                            <input id={preference.name+"-"+index} onChange={this._onPreferenceChanged.bind(this, preference.name)} className="form-control" required value={preference.value} type="text"/>
                        </div>
                    </div>
                );
            }, this)}
            </div>);
        return (
            <BootstrapModal ref="modal" onCancel={this._closeModal} onConfirm={this._save} cancel="Cancel" confirm="Save" formClass="form-horizontal">
               {header}
               {body}
            </BootstrapModal>
        );
    },
    _closeModal() {
        this.refs.modal.close();
    },
    openModal() {
        PreferencesStore.loadUserPreferences(this.props.userName, (preferences) => {
            this.setState({preferences: preferences});
            this.refs.modal.open();
        });
    },
    _save() {
        PreferencesStore.saveUserPreferences(this.state.preferences, this._closeModal);
    }

});

module.exports = UserPreferencesModal;
