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
import { useEffect, useContext } from 'react';
import moment from 'moment';
import 'moment/locale/de';

import i18n from 'i18n';
import UserPreferencesContext from 'contexts/UserPreferencesContext';

const LanguageProvider = ({ children }: React.PropsWithChildren<{}>) => {
  const { language } = useContext(UserPreferencesContext);

  useEffect(() => {
    if (language) {
      i18n.changeLanguage(language);
      moment.locale(language);
    }
  }, [language]);

  return children as React.ReactElement;
};

export default LanguageProvider;
