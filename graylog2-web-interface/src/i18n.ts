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

import { singleton } from 'logic/singleton';
import pseudoLocalize, { PSEUDO_LANGUAGE } from 'util/pseudoLocalize';

type Translations = { [key: string]: unknown };
export type TranslationsLoader = () => Promise<{ default: Translations }>;

const DEFAULT_LANGUAGE = 'en';

// Plugins are separate bundles which compile their own copy of this module. Sharing the instance and the loaders
// through `window.singletons` lets them add their own namespaces and follow language changes of the core.
const i18n = singleton('i18n', () => {
  const instance = i18next.createInstance();

  instance
    .use(initReactI18next)
    .use({
      type: 'postProcessor',
      name: 'pseudo',
      process: (value: string, _key: unknown, _options: unknown, translator: { language: string }) =>
        translator.language === PSEUDO_LANGUAGE ? pseudoLocalize(value) : value,
    })
    .init({
      resources: {},
      postProcess: ['pseudo'],
      lng: DEFAULT_LANGUAGE,
      fallbackLng: DEFAULT_LANGUAGE,
      interpolation: { escapeValue: false },
    });

  return instance;
});

const loadersByLanguage = singleton('i18n.loaders', () => new Map<string, Map<string, TranslationsLoader>>());

const loadTranslations = async (language: string, namespace: string, loader: TranslationsLoader) => {
  if (i18n.hasResourceBundle(language, namespace)) {
    return;
  }

  const { default: translations } = await loader();
  i18n.addResourceBundle(language, namespace, translations);
};

/**
 * Registers the translations of a namespace. English is bundled, because it is the fallback for missing translations.
 * Other languages are loaded on demand, e.g. `{ de: () => import('locales/de/search.json') }`.
 */
export const registerTranslations = (
  namespace: string,
  english: Translations,
  otherLanguages: { [language: string]: TranslationsLoader },
) => {
  i18n.addResourceBundle(DEFAULT_LANGUAGE, namespace, english);

  Object.entries(otherLanguages).forEach(([language, loader]) => {
    if (!loadersByLanguage.has(language)) {
      loadersByLanguage.set(language, new Map());
    }

    loadersByLanguage.get(language).set(namespace, loader);
  });

  // Plugins can register their translations after the language has been changed already.
  const currentLanguageLoader = otherLanguages[i18n.language];

  return currentLanguageLoader ? loadTranslations(i18n.language, namespace, currentLanguageLoader) : Promise.resolve();
};

/** Loads the translations of all registered namespaces before switching, so no texts change twice. */
export const changeLanguage = async (language: string) => {
  const loaders = [...(loadersByLanguage.get(language) ?? new Map<string, TranslationsLoader>())];

  await Promise.all(loaders.map(([namespace, loader]) => loadTranslations(language, namespace, loader)));

  return i18n.changeLanguage(language);
};

export default i18n;
