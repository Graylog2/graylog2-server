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
import i18next from 'i18next';
import { initReactI18next } from 'react-i18next';

import enSearch from 'locales/en/search.json';
import deSearch from 'locales/de/search.json';
import { singleton } from 'logic/singleton';

// Plugins are separate bundles. Sharing the instance through `window.singletons` lets them add their own
// namespaces and follow language changes of the core instead of creating an uninitialized copy.
const i18n = singleton('i18n', () => {
  const instance = i18next.createInstance();

  instance.use(initReactI18next).init({
    resources: {
      en: { search: enSearch },
      de: { search: deSearch },
    },
    lng: 'en',
    fallbackLng: 'en',
    interpolation: { escapeValue: false },
  });

  return instance;
});

export default i18n;
