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

import type { ScopeName, HotkeyCollections, Options, HotkeysEvent } from 'contexts/HotkeysContext';
import useHotkeysContext from 'hooks/useHotkeysContext';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

export const DEFAULT_SPLIT_KEY = ',';
export const DEFAULT_COMBINATION_KEY = '+';
const defaultOptions: ReactHotKeysHookOptions & Options = {
  preventDefault: true,
  enabled: true,
  enableOnContentEditable: false,
  combinationKey: DEFAULT_COMBINATION_KEY,
  splitKey: DEFAULT_SPLIT_KEY,
  keyup: undefined,
  keydown: true,
  description: undefined,
  document: undefined,
  ignoreModifiers: false,
  displayInOverview: undefined,
};

const catchErrors = (hotKeysCollections: HotkeyCollections, actionKey: string, scope: ScopeName) => {
  if (!hotKeysCollections[scope]) {
    throw Error(`Scope "${scope}" does not exist in hotkeys collection.`);
  }

  if (!hotKeysCollections[scope].actions[actionKey]) {
    throw Error(`Action "${actionKey}" does not exist in hotkeys collection of "${scope}" scope.`);
  }
};

export type HotkeysProps = {
  actionKey: string,
  callback?: (event: KeyboardEvent, handler: HotkeysEvent) => unknown,
  scope: ScopeName,
  options?: Options,
  dependencies?: Array<unknown>,
  telemetryAppPathname?: string,
}

const useHotkey = <T extends HTMLElement>({
  actionKey,
  callback,
  scope,
  options,
  dependencies,
  telemetryAppPathname,
}: HotkeysProps) => {
  const location = useLocation();
  const sendTelemetry = useSendTelemetry();

  const {
    hotKeysCollections,
    addActiveHotkey,
    removeActiveHotkey,
  } = useHotkeysContext();

  catchErrors(hotKeysCollections, actionKey, scope);

  const mergedOptions = useMemo(() => ({
    ...defaultOptions,
    ...options,
    scopes: scope,
  }), [options, scope]);

  const callbackWithTelemetry = useCallback((event: KeyboardEvent, handler: HotkeysEvent) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SHORTCUT_TYPED, {
      app_pathname: telemetryAppPathname ?? getPathnameWithoutId(location.pathname),
      event_details: { actionKey, scope, keys: hotKeysCollections?.[scope]?.actions?.[actionKey]?.keys },
    });

    callback(event, handler);
  }, [actionKey, callback, hotKeysCollections, location.pathname, scope, sendTelemetry, telemetryAppPathname]);

  useEffect(() => {
    addActiveHotkey({
      scope,
      actionKey,
      options: {
        scope,
        enabled: mergedOptions.enabled,
        displayInOverview: mergedOptions.displayInOverview,
        combinationKey: mergedOptions.combinationKey,
        splitKey: mergedOptions.splitKey,
      },
    });

    return () => removeActiveHotkey({ scope, actionKey });
  }, [actionKey, addActiveHotkey, scope, removeActiveHotkey, mergedOptions.combinationKey, mergedOptions.enabled, mergedOptions.displayInOverview, mergedOptions.splitKey]);

  return originalUseHotkeys<T>(
    hotKeysCollections?.[scope]?.actions?.[actionKey]?.keys,
    callbackWithTelemetry,
    mergedOptions,
    dependencies,
  );
};

export default useHotkey;
