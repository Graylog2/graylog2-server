/** @jsx React.DOM */

var UserPreferencesModal = React.createClass({
    getInitialState: function () {
        return {preferences: {}};
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
    render: function () {
        var header = <h2>Preferences for user {PreferencesStore.userName}</h2>;
        var body = <pre>{JSON.stringify(this.state.preferences)}</pre>;
        return (
            <BootstrapModal ref="modal" onCancel={this._closeModal} onConfirm={this._save} cancel="Cancel" confirm="Save">
               {header}
               {body}
            </BootstrapModal>
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
