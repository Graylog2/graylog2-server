'use strict';

var React = require('React');
var PreferencesStore = require('../stores/PreferencesStore');
var BootstrapModal = require('./BootstrapModal');

var UserPreferencesModal = React.createClass({
    getInitialState: function () {
        return {preferences: []};
    },
    componentDidMount: function () {
        PreferencesStore.addChangeListener(this._onInputChanged);
        PreferencesStore.on(PreferencesStore.DATA_LOADED_EVENT, this._openModal);
        PreferencesStore.on(PreferencesStore.DATA_SAVED_EVENT, this._closeModal);

    },
    componentWillUnmount: function () {
        PreferencesStore.removeChangeListener(this._onInputChanged);
        PreferencesStore.removeListener(this.DATA_LOADED_EVENT, this._openModal);
        PreferencesStore.removeListener(PreferencesStore.DATA_SAVED_EVENT, this._closeModal);
    },
    _onInputChanged: function() {
        var preferences = PreferencesStore.getPreferences();
        this.setState({preferences: preferences});
    },
    _onPreferenceChanged: function(name, event) {
        var preferenceToChange = this.state.preferences.filter(function(preference) {
            return preference.name === name;
        })[0];
        // TODO: we need the type of the preference to set it properly
        if (preferenceToChange) {
            preferenceToChange.value = event.target.value;
            this.setState({preferences: this.state.preferences});
        }
    },
    render: function () {
        var header = <h2>Preferences for user {PreferencesStore.getUserName()}</h2>;
        // TODO: Add additional row where you can add a new preference
        // TODO: Add delete button
        var body = (<table className="table table-hover">
            <thead>
                <tr><th>Name</th><th>Value</th></tr>
            </thead>
            <tbody>
            {this.state.preferences.map(function (preference, index) {
                return (<tr key={index}><td>{preference.name}</td><td><div className="form-group"><input onChange={this._onPreferenceChanged.bind(this, preference.name)} className="form-control" value={preference.value}/></div></td></tr>);
            }, this)}

            </tbody>
        </table>);
        return (
            <form className="form-inline" role="form">
                <BootstrapModal ref="modal" onCancel={this._closeModal} onConfirm={this._save} cancel="Cancel" confirm="Save">
                   {header}
                   {body}
                </BootstrapModal>
            </form>
        );
    },
    _closeModal: function () {
        this.refs.modal.close();
    },
    _openModal: function () {
        this.refs.modal.open();
    },
    _save: function () {
        PreferencesStore.saveUserPreferences(this.state.preferences);
    }

});

module.exports = UserPreferencesModal;