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
// The preflight is a separate app which doesn't load the core translations. This module is imported by its entry
// point and by `registerCoreTranslations`, so tests, which use the core setup, can resolve its texts too.
import { registerTranslations } from 'i18n';
import enPreflight from 'locales/en/preflight.json';

registerTranslations('preflight', enPreflight, {
  de: () => import(/* webpackChunkName: "translations-de-preflight" */ 'locales/de/preflight.json'),
});
