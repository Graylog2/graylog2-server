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
export type LanguageCode = 'en' | 'de';

export type Language = {
  code: LanguageCode;
  /** Name of the language in the language itself, so users can find theirs regardless of the current UI language. */
  name: string;
  /** Translations which have not been reviewed yet. */
  beta: boolean;
};

export const DEFAULT_LANGUAGE: LanguageCode = 'en';

export const LANGUAGES: Array<Language> = [
  { code: 'en', name: 'English', beta: false },
  { code: 'de', name: 'Deutsch', beta: false },
];

export const getLanguage = (code: string | undefined): Language =>
  LANGUAGES.find((language) => language.code === code) ?? LANGUAGES[0];
