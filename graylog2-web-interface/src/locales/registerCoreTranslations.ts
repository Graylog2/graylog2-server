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
// Only imported by the core entry point, so plugins, which compile core modules into their bundles, don't contain
// a copy of the core translations.
import { registerTranslations } from 'i18n';
import enCommon from 'locales/en/common.json';
import enLogin from 'locales/en/login.json';
import enNavigation from 'locales/en/navigation.json';
import enSearch from 'locales/en/search.json';

registerTranslations('common', enCommon, {
  de: () => import(/* webpackChunkName: "translations-de-common" */ 'locales/de/common.json'),
});

registerTranslations('login', enLogin, {
  de: () => import(/* webpackChunkName: "translations-de-login" */ 'locales/de/login.json'),
});

registerTranslations('navigation', enNavigation, {
  de: () => import(/* webpackChunkName: "translations-de-navigation" */ 'locales/de/navigation.json'),
});

registerTranslations('search', enSearch, {
  de: () => import(/* webpackChunkName: "translations-de-search" */ 'locales/de/search.json'),
});
