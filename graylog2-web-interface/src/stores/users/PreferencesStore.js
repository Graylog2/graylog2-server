// @flow strict
import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import ActionsProvider from 'injection/ActionsProvider';

const PreferencesActions = ActionsProvider.getActions('Preferences');

type Preference = {
  name: string,
  value: any,
};

type PreferencesMap = {
  [index: string]: any,
};

type PreferencesResponse = {
  preferences: PreferencesMap,
};

const PreferencesStore = Reflux.createStore({
  listenables: [PreferencesActions],
  URL: URLUtils.qualifyUrl('/users/'),
  convertPreferenceMapToArray(preferencesAsMap: PreferencesMap): Array<Preference> {
    let preferences = Object.keys(preferencesAsMap)
      .map((name) => {
        return {
          name: name,
          value: preferencesAsMap[name],
        };
      });
    preferences = preferences.sort((t1: Preference, t2: Preference) => t1.name.localeCompare(t2.name));
    return preferences;
  },
  convertPreferenceArrayToMap(preferences: Array<Preference>): PreferencesMap {
    const preferencesAsMap = {};
    preferences.forEach((element) => {
      // TODO: Converting all preferences to booleans for now, we should change this when we support more types
      preferencesAsMap[element.name] = element.value === true || element.value === 'true';
    });
    return preferencesAsMap;
  },
  saveUserPreferences(preferences: Array<Preference>, callback: (preferences: Array<any>) => void): void {
    if (!this._userName) {
      throw new Error('Need to load user preferences before you can save them');
    }
    const preferencesAsMap = this.convertPreferenceArrayToMap(preferences);
    const url = `${this.URL + this._userName}/preferences`;
    const promise = fetch('PUT', url, { preferences: preferencesAsMap })
      .then(() => {
        UserNotification.success('User preferences successfully saved');
        callback(preferences);
      }, (errorThrown) => {
        UserNotification.error(`Saving of preferences for "${this._userName}" failed with status: ${errorThrown}`,
          'Could not save user preferences');
      });

    PreferencesActions.saveUserPreferences.promise(promise);

    return promise;
  },
  loadUserPreferences(userName: string, callback: (preferences: Array<any>) => void): void {
    this._userName = userName;

    const url = this.URL + userName;
    const successCallback = (data: PreferencesResponse) => {
      const sortedArray = this.convertPreferenceMapToArray(data.preferences);
      callback(sortedArray);
    };
    const failCallback = (errorThrown) => {
      UserNotification.error(
        `Loading of user preferences for "${userName}" failed with status: ${errorThrown}. Try reloading the page`,
        'Could not retrieve user preferences from server',
      );
    };
    const promise = fetch('GET', url)
      .then(successCallback, failCallback);

    PreferencesActions.loadUserPreferences.promise(promise);

    return promise;
  },
});

export default PreferencesStore;
