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
import * as React from 'react';
import { useContext } from 'react';

import { NavDropdown, MenuItem } from 'components/bootstrap';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import useCurrentUser from 'hooks/useCurrentUser';
import { saveUserPreferences, type PreferencesUpdateMap } from 'api/preferences';

const LANGUAGES = [
  { value: 'en', label: 'English' },
  { value: 'de', label: 'Deutsch' },
];

const LanguageSwitcher = () => {
  const { language = 'en' } = useContext(UserPreferencesContext);
  const currentUser = useCurrentUser();

  const handleSelect = (newLanguage: string) => {
    if (!currentUser || newLanguage === language) return;

    saveUserPreferences(
      currentUser.username,
      { ...(currentUser.preferences as PreferencesUpdateMap), language: newLanguage },
      () => {},
      false,
    );
  };

  return (
    <NavDropdown title={language.toUpperCase()} hoverTitle="Language" noCaret>
      {LANGUAGES.map(({ value, label }) => (
        <MenuItem key={value} active={language === value} onSelect={() => handleSelect(value)}>
          {label}
        </MenuItem>
      ))}
    </NavDropdown>
  );
};

export default LanguageSwitcher;
