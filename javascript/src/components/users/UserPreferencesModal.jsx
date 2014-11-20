'use strict';

var React = require('react/addons');
var PreferencesStore = require('../../stores/users/PreferencesStore');
var BootstrapModal = require('../bootstrap/BootstrapModal');

var UserPreferencesModal = React.createClass({
    getInitialState() {
        return {preferences: []};
    },
    componentDidMount() {
        PreferencesStore.addChangeListener(this._onInputChanged);
        PreferencesStore.on(PreferencesStore.DATA_LOADED_EVENT, this._openModal);
        PreferencesStore.on(PreferencesStore.DATA_SAVED_EVENT, this._closeModal);

    },
    componentWillUnmount() {
        PreferencesStore.removeChangeListener(this._onInputChanged);
        PreferencesStore.removeListener(this.DATA_LOADED_EVENT, this._openModal);
        PreferencesStore.removeListener(PreferencesStore.DATA_SAVED_EVENT, this._closeModal);
    },
    _onInputChanged() {
        var preferences = PreferencesStore.getPreferences();
        this.setState({preferences: preferences});
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
        var header = <h2>Preferences for user {PreferencesStore.getUserName()}</h2>;
        // TODO: Add additional row where you can add a new preference
        // TODO: Add delete button
        var body = (<table className="table table-hover">
            <thead>
                <tr><th>Name</th><th>Value</th></tr>
            </thead>
            <tbody>
            {this.state.preferences.map((preference, index) => {
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
    _closeModal() {
        this.refs.modal.close();
    },
    _openModal() {
        this.refs.modal.open();
    },
    _save() {
        PreferencesStore.saveUserPreferences(this.state.preferences);
    }

});

module.exports = UserPreferencesModal;
