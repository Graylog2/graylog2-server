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
import type { Hotkey } from 'react-hotkeys-hook/dist/types';
import type { Options } from 'react-hotkeys-hook';
import type Immutable from 'immutable';

import { singleton } from 'logic/singleton';

export type ScopeName = 'general' | 'search' | 'dashboard';
export type ScopeParam = Array<ScopeName> | ScopeName
export type ActiveHotkey = {
  options?: Options & { scopes: ScopeName },
}
export type ActiveHotkeys = Immutable.Map<`${ScopeName}.${string}`, ActiveHotkey>
export type HotkeyCollection = {
  title: string,
  description: string,
  actions: Record<string, { keys: string, description: string }>,
}
export type HotkeyCollections = Record<ScopeName, HotkeyCollection>

type HotkeysContextType = {
  disableScope: (scopes: ScopeParam) => void,
  enableScope: (scopes: ScopeParam) => void,
  setActiveScopes: (scopes: ScopeParam) => void,
  hotkeys: readonly Hotkey[],
  toggleScope: (scopes: ScopeName) => void,
  enabledScopes: Array<ScopeName>,
  hotKeysCollection: HotkeyCollections,
  activeHotkeys: ActiveHotkeys,
  addActiveHotkey: (props: { scope: ScopeName, actionKey: string, options: Options & { scopes: ScopeName } }) => void,
  removeActiveHotkey: (props: { scope: ScopeName, actionKey: string }) => void,
}

const HotkeysContext = React.createContext<HotkeysContextType | undefined>(undefined);

export default singleton('contexts.HotkeysContext', () => HotkeysContext);
