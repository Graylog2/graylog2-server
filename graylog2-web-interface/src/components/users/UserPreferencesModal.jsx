import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';

import StoreProvider from 'injection/StoreProvider';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

const PreferencesStore = StoreProvider.getStore('Preferences');

class UserPreferencesModal extends React.Component {
  static propTypes = {
    userName: PropTypes.string.isRequired,
  };

  state = { preferences: [] };

  _onPreferenceChanged = (event) => {
    const { name } = event.target;
    const preferenceToChange = this.state.preferences.filter((preference) => preference.name === name)[0];
    // TODO: we need the type of the preference to set it properly
    if (preferenceToChange) {
      preferenceToChange.value = event.target.value;
      this.setState({ preferences: this.state.preferences });
    }
  };

  _save = () => {
    PreferencesStore.saveUserPreferences(this.state.preferences, this.modal.close);
  };

  openModal = () => {
    PreferencesStore.loadUserPreferences(this.props.userName, (preferences) => {
      this.setState({ preferences: preferences });
      this.modal.open();
    });
  };

  render() {
    let shouldAutoFocus = true;

    const formattedPreferences = this.state.preferences.map((preference, index) => {
      const formattedPreference = (
        <div className="form-group" key={`${preference.name}-${index}`}>
          <Input type="text"
                 id={`${preference.name}-${index}`}
                 name={preference.name}
                 label={preference.name}
                 onChange={this._onPreferenceChanged}
                 value={preference.value}
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
      <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
                          title={`Preferences for user ${this.props.userName}`}
                          onSubmitForm={this._save}
                          submitButtonText="Save">
        <div>{formattedPreferences}</div>
      </BootstrapModalForm>
    );
  }
}

export default UserPreferencesModal;
