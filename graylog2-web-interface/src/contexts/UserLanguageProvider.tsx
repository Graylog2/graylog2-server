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

import UserPreferencesContext from 'contexts/UserPreferencesContext';
import useCurrentUser from 'hooks/useCurrentUser';
import { lastLanguage } from 'util/applyLanguage';
import { DEFAULT_LANGUAGE } from 'components/users/languages/Languages';

import LanguageProvider from './LanguageProvider';

/**
 * Applies the language of the current user. Users without one get the default, not the previous user's language.
 * Read-only users, like the built-in admin, can't save preferences. They choose their language in the user menu,
 * which keeps it in the browser, so that choice must not be replaced by the default.
 */
const UserLanguageProvider = ({ children }: React.PropsWithChildren) => {
  const currentUser = useCurrentUser();
  const { language } = useContext(UserPreferencesContext);
  const userLanguage = currentUser?.readOnly ? lastLanguage() : (language ?? DEFAULT_LANGUAGE);

  return <LanguageProvider language={userLanguage}>{children}</LanguageProvider>;
};

export default UserLanguageProvider;
