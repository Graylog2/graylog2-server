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

import type { HotkeyCallback, Options } from 'react-hotkeys-hook';
import { useHotkeys as originalUseHotkeys } from 'react-hotkeys-hook';
import { useEffect, useMemo } from 'react';

import type { ScopeName, HotkeyCollections } from 'contexts/HotkeysContext';
import useHotkeysContext from 'hooks/useHotkeysContext';

const defaultOptions: Options = {
  preventDefault: true,
  enabled: true,
  enableOnFormTags: false,
  enableOnContentEditable: false,
  combinationKey: '+',
  splitKey: ',',
  scopes: '*',
  keyup: undefined,
  keydown: true,
  description: undefined,
  document: undefined,
  ignoreModifiers: false,
};

const catchErrors = (hotKeysCollections: HotkeyCollections, actionKey: string, scope: ScopeName) => {
  if (!hotKeysCollections[scope]) {
    throw Error(`Scope "${scope}" does not exist in hotkeys collection.`);
  }

  if (!hotKeysCollections[scope].actions[actionKey]) {
    throw Error(`Action "${actionKey}" does not exist in hotkeys collection of "${scope}" scope.`);
  }
};

const useHotkeys = <T extends HTMLElement>(
  actionKey: string,
  callback: HotkeyCallback,
  options?: Options & { scopes: ScopeName },
  dependencies?: Array<unknown>,
) => {
  const {
    hotKeysCollections,
    addActiveHotkey,
    removeActiveHotkey,
  } = useHotkeysContext();

  catchErrors(hotKeysCollections, actionKey, options.scopes);

  // const scope = options?.scopes ?? '*';
  // const scopes = isArray(scope) ? scope : [scope];
  const mergedOptions = useMemo(() => ({
    ...defaultOptions,
    ...options,
  }), [options]);

  /*
  useEffect(() => {
    setHotKeysCollection((cur) => {
      const newState = { ...cur };

      scopes.forEach((curScope) => {
        set(newState, curScope, { keys, description: options?.description });
      });
    });
  }, [keys, options?.description, scopes, setHotKeysCollection]);
*/
  useEffect(() => {
    addActiveHotkey({
      scope: options.scopes,
      actionKey,
      options: {
        scopes: options.scopes,
        enabled: options.enabled,
        combinationKey: options.combinationKey,
      },
    });

    return () => removeActiveHotkey({ scope: options.scopes, actionKey });
  }, [actionKey, addActiveHotkey, options.combinationKey, options.enabled, options.scopes, removeActiveHotkey]);

  const keys = hotKeysCollections?.[options?.scopes]?.actions?.[actionKey]?.keys;

  return originalUseHotkeys<T>(keys, callback, mergedOptions, dependencies);
};

export default useHotkeys;
