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

import type { Options as ReactHotKeysHookOptions } from 'react-hotkeys-hook';
import { useHotkeys as originalUseHotkeys } from 'react-hotkeys-hook';
import { useEffect, useMemo, useCallback } from 'react';

import type { ScopeName, HotkeyCollections, Options } from 'contexts/HotkeysContext';
import useHotkeysContext from 'hooks/useHotkeysContext';
import useFeature from 'hooks/useFeature';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const defaultOptions: ReactHotKeysHookOptions = {
  preventDefault: true,
  enabled: true,
  enableOnFormTags: false,
  enableOnContentEditable: false,
  combinationKey: '+',
  splitKey: ',',
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
  scope,
  options,
  dependencies,
  telemetryAppPathname,
}: {
  actionKey: string,
  callback: () => unknown,
  scope: ScopeName,
  options?: Options,
  dependencies?: Array<unknown>,
  telemetryAppPathname: string,
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

  catchErrors(hotKeysCollections, actionKey, scope);

  // eslint-disable-next-line react-hooks/rules-of-hooks
  const mergedOptions = useMemo(() => ({
    ...defaultOptions,
    ...options,
    scopes: scope,
  }), [options, scope]);

  // eslint-disable-next-line react-hooks/rules-of-hooks
  const callbackWithTelemetry = useCallback(() => {
    sendTelemetry('hotkey_usage', {
      app_pathname: telemetryAppPathname,
      app_section: scope,
      app_action_value: actionKey,
    });

    callback();
  }, [actionKey, callback, scope, sendTelemetry, telemetryAppPathname]);

  // eslint-disable-next-line react-hooks/rules-of-hooks
  useEffect(() => {
    addActiveHotkey({
      scope,
      actionKey,
      options: {
        scope,
        enabled: mergedOptions.enabled,
        combinationKey: mergedOptions.combinationKey,
      },
    });

    return () => removeActiveHotkey({ scope, actionKey });
  }, [actionKey, addActiveHotkey, scope, removeActiveHotkey, mergedOptions.combinationKey, mergedOptions.enabled]);

  const keys = hotKeysCollections?.[scope]?.actions?.[actionKey]?.keys;

  return originalUseHotkeys<T>(keys, callbackWithTelemetry, mergedOptions, dependencies);
};

export default useHotkey;
