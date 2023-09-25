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
import Immutable from 'immutable';

import type { ScopeName, ActiveHotkeys, HotkeyCollections, Options } from 'contexts/HotkeysContext';
import HotkeysContext from 'contexts/HotkeysContext';
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
  const { enabledScopes } = useOriginalHotkeysContext();

  const addActiveHotkey = useCallback(({ scope, actionKey, options }: {
    scope: ScopeName,
    actionKey: string,
    options: Options & { scope: ScopeName }
  }) => {
    setActiveHotkeys((cur) => cur.set(`${scope}.${actionKey}`, { options }));
  }, []);

  const removeActiveHotkey = useCallback(({ scope, actionKey }: { scope: ScopeName, actionKey: string }) => {
    setActiveHotkeys((cur) => cur.delete(`${scope}.${actionKey}`));
  }, []);

  const value = useMemo(() => ({
    enabledScopes: enabledScopes as Array<ScopeName>,
    hotKeysCollections,
    activeHotkeys,
    addActiveHotkey,
    removeActiveHotkey,
  }), [
    activeHotkeys,
    addActiveHotkey,
    enabledScopes,
    removeActiveHotkey,
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
      </CustomHotkeysProvider>
    </OriginalHotkeysProvider>
  );
};

export default HotkeysProvider;
