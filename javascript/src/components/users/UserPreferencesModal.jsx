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
        var body = (<table className="table table-hover">
            <thead>
                <tr><th>Name</th><th>Value</th></tr>
            </thead>
            <tbody>
            {this.state.preferences.map((preference, index) => {
                return (<tr key={index}><td>{preference.name}</td><td><div className="form-group"><input onChange={this._onPreferenceChanged.bind(this, preference.name)} className="form-control" required value={preference.value}/></div></td></tr>);
            }, this)}

            </tbody>
        </table>);
        return (
            <BootstrapModal ref="modal" onCancel={this._closeModal} onConfirm={this._save} cancel="Cancel" confirm="Save">
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
