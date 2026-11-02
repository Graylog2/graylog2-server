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
import 'moment/locale/es';
import 'moment/locale/fr';
import 'moment/locale/pl';
import 'moment/locale/pt-br';
import 'moment/locale/zh-cn';
import 'moment/locale/zh-tw';
import 'moment/locale/ja';
import 'numeral/locales/de';
import 'numeral/locales/es';
import 'numeral/locales/fr';
import 'numeral/locales/pl';
import 'numeral/locales/pt-br';
import 'numeral/locales/chs';
import 'numeral/locales/ja';

import { changeLanguage } from 'i18n';
import Store from 'logic/local-storage/Store';
import { DEFAULT_LANGUAGE } from 'components/users/languages/Languages';

const LAST_LANGUAGE_KEY = 'language';

// numeral names some locales differently than the language codes
const NUMERAL_LOCALES: { [language: string]: string } = { 'pt-BR': 'pt-br', 'zh-CN': 'chs' };

/** Language of the last logged in user, used for pages shown before login. */
export const lastLanguage = () => Store.get<string>(LAST_LANGUAGE_KEY) ?? DEFAULT_LANGUAGE;

const applyLanguage = async (language: string) => {
  await changeLanguage(language);
  moment.locale(language);

  const numeralLocale = NUMERAL_LOCALES[language] ?? language;

  // languages without a numeral locale (e.g. zh-TW) use the default format instead of keeping the previous language's
  numeral.locale(numeral.locales[numeralLocale] ? numeralLocale : DEFAULT_LANGUAGE);

  Store.set(LAST_LANGUAGE_KEY, language);
};

export default applyLanguage;
