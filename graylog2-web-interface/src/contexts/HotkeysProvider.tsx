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

const viewActions = {
  undo: { keys: 'mod+shift+z', description: 'Undo last action' },
  redo: { keys: 'mod+shift+y', description: 'Redo last action' },
};

export const hotKeysCollections: HotkeyCollections = {
  general: {
    title: 'General',
    description: 'General keyboard shortcuts',
    actions: {
      'show-hotkeys-modal': { keys: 'shift+?', displayKeys: '?', description: 'Show available keyboard shorts' },
      'submit-form': { keys: 'enter', description: 'Submit form' },
      'close-modal': { keys: 'esc', description: 'Close modal' },
      'show-scratchpad-modal': { keys: 'mod+/', description: 'Show scratchpad' },
    },
  },
  search: {
    title: 'Search',
    description: 'Keyboard shortcuts for search page',
    actions: {
      ...viewActions,
      save: { keys: 'mod+s', description: 'Save search' },
      'save-as': { keys: 'mod+shift+s', description: 'Save search as' },
    },
  },
  dashboard: {
    title: 'Dashboard',
    description: 'Keyboard shortcuts for dashboard page',
    actions: {
      ...viewActions,
      save: { keys: 'mod+s', description: 'Save dashboard' },
      'save-as': { keys: 'mod+shift+s', description: 'Save dashboard as' },
    },
  },
  'query-input': {
    title: 'Query Input',
    description: 'Keyboard shortcuts for query input in search bar, available when input is focussed.',
    // Please note, any changes to keybindings also need to be made in the query input component.
    actions: {
      'submit-search': { keys: 'return', description: 'Execute the search' },
      'insert-newline': { keys: 'shift+return', description: 'Create a new line' },
      'create-search-filter': { keys: 'alt+return', description: 'Create search filter based on current query' },
      'show-suggestions': { keys: 'alt+space', description: 'Show suggestions, displays query history when input is empty' },
      'show-history': { keys: 'alt+shift+h', description: 'View your search query history' },
    },
  },
  scratchpad: {
    title: 'Scratchpad',
    description: 'Scratchpad shortcuts',
    actions: {
      clear: { keys: ['mod+backspace', 'mod+del'], description: 'Clear scratchpad' },
      copy: { keys: 'shift+mod+c', description: 'Copy scratchpad' },
    },
  },
};

const CustomHotkeysProvider = ({ children }: PropsWithChildren) => {
  const [activeHotkeys, setActiveHotkeys] = useState<ActiveHotkeys>(Immutable.Map());
  const { enabledScopes } = useOriginalHotkeysContext();
  const [showHotkeysModal, setShowHotkeysModal] = useState(false);

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
    showHotkeysModal,
    setShowHotkeysModal,
  }), [activeHotkeys, addActiveHotkey, enabledScopes, removeActiveHotkey, showHotkeysModal]);

  return (
    <HotkeysContext.Provider value={value}>
      {children}
    </HotkeysContext.Provider>
  );
};

type Props = {
  children: React.ReactElement,
}

const HotkeysProvider = ({ children }: Props) => (
  <OriginalHotkeysProvider>
    <CustomHotkeysProvider>
      {children}
    </CustomHotkeysProvider>
  </OriginalHotkeysProvider>
);

export default HotkeysProvider;
