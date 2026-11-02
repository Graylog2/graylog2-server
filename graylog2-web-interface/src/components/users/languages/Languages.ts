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
import AppConfig from 'util/AppConfig';
import { PSEUDO_LANGUAGE } from 'util/pseudoLocalize';

export type LanguageCode = 'en' | 'de' | 'es' | 'fr' | 'pt-BR' | 'zh-CN' | typeof PSEUDO_LANGUAGE;

export type Language = {
  code: LanguageCode;
  /** Name of the language in the language itself, so users can find theirs regardless of the current UI language. */
  name: string;
  /** Translations which have not been reviewed yet. */
  beta: boolean;
  /** Only offered in development mode. */
  developmentOnly?: boolean;
};

export const DEFAULT_LANGUAGE: LanguageCode = 'en';

export const LANGUAGES: Array<Language> = [
  { code: 'en', name: 'English', beta: false },
  { code: 'de', name: 'Deutsch', beta: true },
  { code: 'es', name: 'Español', beta: true },
  { code: 'fr', name: 'Français', beta: true },
  { code: 'pt-BR', name: 'Português (Brasil)', beta: true },
  { code: 'zh-CN', name: '简体中文', beta: true },
  { code: PSEUDO_LANGUAGE, name: 'Pseudo', beta: false, developmentOnly: true },
];

export const availableLanguages = () =>
  LANGUAGES.filter(({ developmentOnly }) => !developmentOnly || AppConfig.gl2DevMode());

export const getLanguage = (code: string | undefined): Language =>
  LANGUAGES.find((language) => language.code === code) ?? LANGUAGES[0];
