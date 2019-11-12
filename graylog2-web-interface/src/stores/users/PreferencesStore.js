import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const PreferencesStore = Reflux.createStore({
  URL: URLUtils.qualifyUrl('/users/'),
  convertPreferenceMapToArray(preferencesAsMap = {}) {
    let preferences = Object.keys(preferencesAsMap)
      .map((name) => {
        return {
          name: name,
          value: preferencesAsMap[name],
        };
      });
    preferences = preferences.sort((t1, t2) => t1.name.localeCompare(t2.name));
    return preferences;
  },
  convertPreferenceArrayToMap(preferences) {
    const preferencesAsMap = {};
    preferences.forEach((element) => {
      // TODO: Converting all preferences to booleans for now, we should change this when we support more types
      preferencesAsMap[element.name] = element.value === true || element.value === 'true';
    });
    return preferencesAsMap;
  },
  saveUserPreferences(preferences, callback) {
    if (!this._userName) {
      throw new Error('Need to load user preferences before you can save them');
    }
    const preferencesAsMap = this.convertPreferenceArrayToMap(preferences);
    const url = `${this.URL + this._userName}/preferences`;
    fetch('PUT', url, { preferences: preferencesAsMap })
      .then(() => {
        UserNotification.success('User preferences successfully saved');
        callback(preferences);
      }, (errorThrown) => {
        UserNotification.error(`Saving of preferences for "${this._userName}" failed with status: ${errorThrown}`,
          'Could not save user preferences');
      });
  },
  loadUserPreferences(userName, callback) {
    this._userName = userName;

    const url = this.URL + userName;
    const successCallback = (data) => {
      const sortedArray = this.convertPreferenceMapToArray(data.preferences);
      callback(sortedArray);
    };
    const failCallback = (errorThrown) => {
      UserNotification.error(
        `Loading of user preferences for "${userName}" failed with status: ${errorThrown}. Try reloading the page`,
        'Could not retrieve user preferences from server',
      );
    };
    fetch('GET', url)
      .then(successCallback, failCallback);
  },
});

export default PreferencesStore;
