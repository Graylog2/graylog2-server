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
import Reflux from 'reflux';
import type { ColorScheme } from '@graylog/sawmill';

import type { PREFERENCES_THEME_MODE } from 'theme/constants';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import { singletonStore, singletonActions } from 'logic/singleton';

type PreferencesActionsType = {
  saveUserPreferences: (userName: string, preferences: PreferencesUpdateMap, callback?: (preferences: PreferencesUpdateMap) => void, displaySuccessNotification?: boolean) => Promise<unknown>,
}
export const PreferencesActions = singletonActions(
  'core.Preferences',
  () => Reflux.createActions<PreferencesActionsType>({
    loadUserPreferences: { asyncResult: true },
    saveUserPreferences: { asyncResult: true },
  }),
);

type BooleanString = 'true' | 'false'

export type PreferencesUpdateMap = {
  enableSmartSearch: boolean | BooleanString,
  updateUnfocussed: boolean | BooleanString,
  dashboardSidebarIsPinned?: boolean | BooleanString,
  searchSidebarIsPinned?: boolean | BooleanString,
  [PREFERENCES_THEME_MODE]?: ColorScheme,
};

export type PreferencesMap = {
  enableSmartSearch: boolean,
  updateUnfocussed: boolean,
  dashboardSidebarIsPinned?: boolean,
  searchSidebarIsPinned?: boolean,
  [PREFERENCES_THEME_MODE]: ColorScheme,
};

const convertPreferences = (preferences: PreferencesUpdateMap): PreferencesMap => {
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

  // @ts-ignore
  return convertedPreferences;
};

export const PreferencesStore = singletonStore(
  'core.Preferences',
  () => Reflux.createStore({
    listenables: [PreferencesActions],
    URL: qualifyUrl('/users/'),

    saveUserPreferences(userName: string, preferences: PreferencesUpdateMap, callback: (preferences: PreferencesUpdateMap) => void = () => {}, displaySuccessNotification = true) {
      const convertedPreferences = convertPreferences(preferences);
      const url = `${this.URL + encodeURIComponent(userName)}/preferences`;
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
  }),
);
