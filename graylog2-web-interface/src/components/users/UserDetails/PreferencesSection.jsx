// @flow strict
import * as React from 'react';

import { PREFERENCES_THEME_MODE } from 'theme/constants';
import Store from 'logic/local-storage/Store';
import User from 'logic/users/User';
import ReadOnlyFormGroup from 'components/common/ReadOnlyFormGroup';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  user: User,
};

export const getPreferenceValueLabel = (preferenceType: string, preferenceValue: mixed): string => {
  if (preferenceType === 'boolean') {
    return preferenceValue ? 'yes' : 'no';
  }

  return String(preferenceValue);
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
      <ReadOnlyFormGroup label="Search autocompletion" value={getPreferenceValueLabel('boolean', enableSmartSearch)} />
      <ReadOnlyFormGroup label="Update unfocused" value={getPreferenceValueLabel('boolean', updateUnfocussed)} />
      <ReadOnlyFormGroup label="Pin search sidebar" value={getPreferenceValueLabel('boolean', searchSidebarIsPinned)} />
      <ReadOnlyFormGroup label="Pin dashboard sidebar" value={getPreferenceValueLabel('boolean', dashboardSidebarIsPinned)} />
      <ReadOnlyFormGroup label="Theme mode" value={getPreferenceValueLabel('enum', preferences?.[PREFERENCES_THEME_MODE] ?? 'Not configured')} />
    </SectionComponent>
  );
};

export default PreferencesSection;
