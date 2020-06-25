import * as React from 'react';
import PropTypes from 'prop-types';

import { Input } from 'components/bootstrap';
import StoreProvider from 'injection/StoreProvider';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

const PreferencesStore = StoreProvider.getStore('Preferences');

class UserPreferencesModal extends React.Component {
  static propTypes = {
    userName: PropTypes.string.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = { preferences: [] };
  }

  _onPreferenceChanged = (event) => {
    const { name } = event.target;
    const { preferences } = this.state;
    const preferenceToChange = preferences.filter((preference) => preference.name === name)[0];

    // TODO: we need the type of the preference to set it properly
    if (preferenceToChange) {
      preferenceToChange.value = event.target.value;

      this.setState({ preferences: preferences });
    }
  };

  _save = () => {
    const { userName } = this.props;
    const { preferences } = this.state;

    PreferencesStore.saveUserPreferences(userName, preferences, this.modal.close);
  };

  openModal = () => {
    const { userName } = this.props;

    PreferencesStore.loadUserPreferences(userName, (preferences) => {
      this.setState({ preferences: preferences });

      this.modal.open();
    });
  };

  render() {
    const { userName } = this.props;
    const { preferences } = this.state;
    let shouldAutoFocus = true;

    const formattedPreferences = preferences.map((preference, index) => {
      const formattedPreference = (
        // eslint-disable-next-line react/no-array-index-key
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
                          title={`Preferences for user ${userName}`}
                          onSubmitForm={this._save}
                          submitButtonText="Save">
        <div>{formattedPreferences}</div>
      </BootstrapModalForm>
    );
  }
}

export default UserPreferencesModal;
