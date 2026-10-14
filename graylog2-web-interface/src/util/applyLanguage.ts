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
import moment from 'moment';
import numeral from 'numeral';
import 'moment/locale/de';
import 'numeral/locales/de';

import { changeLanguage } from 'i18n';
import Store from 'logic/local-storage/Store';
import { DEFAULT_LANGUAGE } from 'components/users/languages/Languages';

const LAST_LANGUAGE_KEY = 'language';

/** Language of the last logged in user, used for pages shown before login. */
export const lastLanguage = () => Store.get<string>(LAST_LANGUAGE_KEY) ?? DEFAULT_LANGUAGE;

const applyLanguage = async (language: string) => {
  await changeLanguage(language);
  moment.locale(language);

  if (numeral.locales[language]) {
    numeral.locale(language);
  }

  Store.set(LAST_LANGUAGE_KEY, language);
};

export default applyLanguage;
