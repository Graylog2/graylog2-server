/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import Reflux from 'reflux';

import type { PreferencesThemeMode, ThemeMode } from 'theme/constants';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import ActionsProvider from 'injection/ActionsProvider';

const PreferencesActions = ActionsProvider.getActions('Preferences');

export type Preference = {
  name: string,
  value: boolean | string,
};

export type PreferencesMap = {
  enableSmartSearch: boolean,
  updateUnfocussed: boolean,
  dashboardSidebarIsPinned?: boolean,
  searchSidebarIsPinned?: boolean,
  [PreferencesThemeMode]: ThemeMode,
};

type PreferencesResponse = {
  preferences: PreferencesMap,
};

const convertPreferences = (preferences: PreferencesMap): PreferencesMap => {
  const convertedPreferences = {};

  Object.entries(preferences).forEach(([key, value]) => {
    if (value === 'true') {
      convertedPreferences[key] = true;
    } else if (value === 'false') {
      convertedPreferences[key] = false;
    } else {
      convertedPreferences[key] = value;
    }
  });

  return convertedPreferences;
};

const PreferencesStore = Reflux.createStore({
  listenables: [PreferencesActions],
  URL: qualifyUrl('/users/'),

  saveUserPreferences(userName: string, preferences: PreferencesMap, callback: (preferences: PreferencesMap) => void = () => {}, displaySuccessNotification: boolean = true): void {
    const convertedPreferences = convertPreferences(preferences);
    const url = `${this.URL + userName}/preferences`;
    const promise = fetch('PUT', url, { preferences: convertedPreferences })
      .then(() => {
        if (displaySuccessNotification) {
          UserNotification.success('User preferences successfully saved');
        }

        callback(preferences);
      }, (errorThrown) => {
        UserNotification.error(`Saving of preferences for "${userName}" failed with status: ${errorThrown}`,
          'Could not save user preferences');
      });

    PreferencesActions.saveUserPreferences.promise(promise);

    return promise;
  },
  loadUserPreferences(userName: string, callback: (preferences: PreferencesMap) => void = () => {}): void {
    const url = this.URL + userName;

    const failCallback = (errorThrown) => {
      UserNotification.error(
        `Loading of user preferences for "${userName}" failed with status: ${errorThrown}. Try reloading the page`,
        'Could not retrieve user preferences from server',
      );
    };

    const promise = fetch('GET', url).then((data: PreferencesResponse) => callback(data?.preferences), failCallback);

    PreferencesActions.loadUserPreferences.promise(promise);

    return promise;
  },
});

export default PreferencesStore;
