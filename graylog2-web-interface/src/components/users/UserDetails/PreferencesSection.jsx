// @flow strict
import * as React from 'react';

import { PREFERENCES_THEME_MODE } from 'theme/constants';
import { ReadOnlyFormGroup } from 'components/common';
import Store from 'logic/local-storage/Store';
import User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  user: User,
};

const PreferencesSection = ({ user: { preferences: databasePreferences, readOnly } }: Props) => {
  let preferences = databasePreferences || {};

  if (readOnly) {
    const localStoragePreferences = {
      searchSidebarIsPinned: Store.get('searchSidebarIsPinned'),
      dashboardSidebarIsPinned: Store.get('dashboardSidebarIsPinned'),
      [PREFERENCES_THEME_MODE]: Store.get(PREFERENCES_THEME_MODE),
    };
    preferences = { ...preferences, ...localStoragePreferences };
  }

  const {
    enableSmartSearch,
    updateUnfocussed,
    searchSidebarIsPinned,
    dashboardSidebarIsPinned,
  } = preferences;

  return (
    <SectionComponent title="Preferences">
      <ReadOnlyFormGroup label="Search autocompletion" value={enableSmartSearch ?? false} />
      <ReadOnlyFormGroup label="Update unfocused" value={updateUnfocussed ?? false} />
      <ReadOnlyFormGroup label="Pin search sidebar" value={searchSidebarIsPinned ?? false} />
      <ReadOnlyFormGroup label="Pin dashboard sidebar" value={dashboardSidebarIsPinned ?? false} />
      <ReadOnlyFormGroup label="Theme mode" value={preferences?.[PREFERENCES_THEME_MODE] ?? 'Not configured'} />
    </SectionComponent>
  );
};

export default PreferencesSection;
