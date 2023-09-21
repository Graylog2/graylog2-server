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

import type { Options } from 'react-hotkeys-hook';
import { useHotkeys as originalUseHotkeys } from 'react-hotkeys-hook';
import { useEffect, useMemo } from 'react';

import type { ScopeName, HotkeyCollections } from 'contexts/HotkeysContext';
import useHotkeysContext from 'hooks/useHotkeysContext';
import useFeature from 'hooks/useFeature';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

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

const useHotkey = <T extends HTMLElement>({
  actionKey,
  callback,
  telemetryAppPathname,
  options,
  dependencies,
}: {
  actionKey: string,
  callback: () => unknown,
  telemetryAppPathname: string,
  options?: Options & { scopes: ScopeName },
  dependencies?: Array<unknown>,
}) => {
  const hasHotkeysFeatureFlag = useFeature('frontend_hotkeys');

  if (!hasHotkeysFeatureFlag) {
    return null;
  }

  // eslint-disable-next-line react-hooks/rules-of-hooks
  const sendTelemetry = useSendTelemetry();
  const {
    hotKeysCollections,
    addActiveHotkey,
    removeActiveHotkey,
    // eslint-disable-next-line react-hooks/rules-of-hooks
  } = useHotkeysContext();

  catchErrors(hotKeysCollections, actionKey, options.scopes);

  // const scope = options?.scopes ?? '*';
  // const scopes = isArray(scope) ? scope : [scope];

  // eslint-disable-next-line react-hooks/rules-of-hooks
  const mergedOptions = useMemo(() => ({
    ...defaultOptions,
    ...options,
  }), [options]);

  const callbackWithTelemetry = () => {
    sendTelemetry('hotkey_usage', {
      app_pathname: telemetryAppPathname,
      app_section: options.scopes,
      app_action_value: actionKey,
    });

    callback();
  };

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
  // eslint-disable-next-line react-hooks/rules-of-hooks
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

  return originalUseHotkeys<T>(keys, callbackWithTelemetry, mergedOptions, dependencies);
};

export default useHotkey;
