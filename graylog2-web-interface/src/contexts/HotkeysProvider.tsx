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
import * as React from 'react';
import type { PropsWithChildren } from 'react';
import { useCallback, useMemo, useState } from 'react';
import {
  HotkeysProvider as OriginalHotkeysProvider,
  useHotkeysContext as useOriginalHotkeysContext,
} from 'react-hotkeys-hook';
import isArray from 'lodash/isArray';
import Immutable from 'immutable';

import type { ScopeName, ScopeParam, ActiveHotkeys, HotkeyCollections, Options, Hotkey } from 'contexts/HotkeysContext';
import HotkeysContext from 'contexts/HotkeysContext';
import HotkeysModal from 'contexts/HotkeysModal';
import useFeature from 'hooks/useFeature';

const viewActions = {
  undo: { keys: 'mod+z', description: 'Undo last action' },
  redo: { keys: 'mod+y', description: 'Redo last action' },
};

export const hotKeysCollections: HotkeyCollections = {
  general: {
    title: 'General',
    description: 'General keyboard shortcuts',
    actions: {
      'show-hotkeys-modal': { keys: 'shift+?', description: 'Show available keyboard shorts' },
    },
  },
  search: {
    title: 'Search',
    description: 'Keyboard shortcuts for search page',
    actions: {
      ...viewActions,
      save: { keys: 'mod+s', description: 'Save search' },
      'save-as': { keys: 'shift+mod+s', description: 'Save search as' },
    },
  },
  dashboard: {
    title: 'Dashboard',
    description: 'Keyboard shortcuts for dashboard page',
    actions: {
      ...viewActions,
      save: { keys: 'mod+s', description: 'Save dashboard' },
    },
  },
};

const CustomHotkeysProvider = ({ children }: PropsWithChildren) => {
  const [activeHotkeys, setActiveHotkeys] = useState<ActiveHotkeys>(Immutable.Map());
  const { enabledScopes, hotkeys, disableScope, toggleScope, enableScope } = useOriginalHotkeysContext();
  const _enableScope = useCallback((scopes: ScopeParam) => {
    const newScopes = isArray(scopes) ? scopes : [scopes];
    newScopes.forEach((scope) => enableScope(scope));
  }, [enableScope]);

  const _disableScope = useCallback((scopes: ScopeParam) => {
    const toDisableScopes = isArray(scopes) ? scopes : [scopes];
    toDisableScopes.forEach((scope) => disableScope(scope));
  }, [disableScope]);

  const setActiveScopes = useCallback((scopes: ScopeParam) => {
    const scopeToDisable = (enabledScopes as Array<ScopeName>).filter((item) => (isArray(scopes) ? scopes : [scopes]).includes(item));
    _disableScope(scopeToDisable);

    _enableScope(scopes);
  }, [_disableScope, _enableScope, enabledScopes]);

  const addActiveHotkey = useCallback(({ scope, actionKey, options }: { scope: ScopeName, actionKey: string, options: Options }) => {
    setActiveHotkeys((cur) => cur.set(`${scope}.${actionKey}`, { options }));
  }, []);

  const removeActiveHotkey = useCallback(({ scope, actionKey }: { scope: ScopeName, actionKey: string }) => {
    setActiveHotkeys((cur) => cur.delete(`${scope}.${actionKey}`));
  }, []);

  const value = useMemo(() => ({
    disableScope: _disableScope,
    enableScope: _enableScope,
    setActiveScopes,
    hotkeys: hotkeys as Array<Hotkey>,
    toggleScope,
    enabledScopes: enabledScopes as Array<ScopeName>,
    hotKeysCollections,
    activeHotkeys,
    addActiveHotkey,
    removeActiveHotkey,
  }), [
    _disableScope,
    _enableScope,
    activeHotkeys,
    addActiveHotkey,
    enabledScopes,
    hotkeys,
    removeActiveHotkey,
    setActiveScopes,
    toggleScope,
  ]);

  return (
    <HotkeysContext.Provider value={value}>
      {children}
    </HotkeysContext.Provider>
  );
};

type Props = {
  children: React.ReactElement,
}

const HotkeysProvider = ({ children }: Props) => {
  const hasHotkeysFeatureFlag = useFeature('frontend_hotkeys');

  if (!hasHotkeysFeatureFlag) {
    return children;
  }

  return (
    <OriginalHotkeysProvider>
      <CustomHotkeysProvider>
        {children}
        <HotkeysModal />
      </CustomHotkeysProvider>
    </OriginalHotkeysProvider>
  );
};

export default HotkeysProvider;
