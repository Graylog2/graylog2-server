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
import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import type { TOptions } from 'i18next';

import i18n from 'i18n';

/**
 * Texts which the server sends (labels and descriptions of configuration fields, column titles of tables) are English.
 * They are translated with a catalog which uses the English text itself as key, so no ids are needed and a text
 * without translation (or one that changed on the server) simply stays as the server sent it.
 *
 * The English texts contain dots and colons, so the key and namespace separators of i18next are switched off.
 */
const LOOKUP_OPTIONS = { keySeparator: false, nsSeparator: false } as const;

/** Namespaces searched for server texts, in this order. Plugins (e.g. enterprise) register their own. */
const SERVER_TEXT_NAMESPACES = [
  'server_fields',
  'enterprise_server_fields',
  'entity_attributes',
  'enterprise_entity_attributes',
];

const lookup =
  (translate: (key: string, options: TOptions) => string, exists: (key: string, options: TOptions) => boolean) =>
  (namespaces: Array<string>, text: string | undefined): string | undefined => {
    if (!text) {
      return text;
    }

    const namespace = namespaces.find((ns) => exists(text, { ns, ...LOOKUP_OPTIONS }));

    return namespace ? translate(text, { ns: namespace, ...LOOKUP_OPTIONS }) : text;
  };

/** Translated text of a server text, or the text itself. Use `useServerText` in components, so they follow language changes. */
export const translateServerText = (text: string | undefined, namespaces: Array<string> = SERVER_TEXT_NAMESPACES) =>
  lookup(
    (key, options) => i18n.t(key, options) as string,
    (key, options) => i18n.exists(key, options),
  )(namespaces, text);

const useServerText = (namespaces: Array<string> = SERVER_TEXT_NAMESPACES) => {
  const { t, i18n: instance } = useTranslation(namespaces);

  return useCallback(
    (text: string | undefined) =>
      lookup(
        (key, options) => t(key, options) as string,
        (key, options) => instance.exists(key, options),
      )(namespaces, text),
    // `t` changes with the language, which makes the components using this hook render again
    [t, instance, namespaces],
  );
};

export default useServerText;
