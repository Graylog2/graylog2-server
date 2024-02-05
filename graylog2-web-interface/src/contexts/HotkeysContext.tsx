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
import type Immutable from 'immutable';

import { singleton } from 'logic/singleton';

export type DefaultScopeName = '*';
export type ScopeName = 'general' | 'search' | 'dashboard' | 'scratchpad' | 'query-input';
export type ScopeParam = Array<ScopeName> | ScopeName

export type KeyboardModifiers = {
  alt?: boolean
  ctrl?: boolean
  meta?: boolean
  shift?: boolean
  mod?: boolean
}

export type Hotkey = KeyboardModifiers & {
  keys: readonly string[]
  scopes: ScopeName,
  description: string
}

export type HotkeysEvent = Hotkey
export type FormTags = 'input' | 'textarea' | 'select' | 'INPUT' | 'TEXTAREA' | 'SELECT'
export type HotkeyCallback = (keyboardEvent: KeyboardEvent, hotkeysEvent: HotkeysEvent) => void

export type Trigger = boolean | ((keyboardEvent: KeyboardEvent, hotkeysEvent: HotkeysEvent) => boolean)
export type Options = {
  combinationKey?: string // Character to split keys in hotkeys combinations. (Default: +)
  enabled?: Trigger // Main setting that determines if the hotkey is enabled or not. (Default: true)
  enableOnFormTags?: readonly FormTags[] | boolean // Enable hotkeys on a list of tags. (Default: false)
  enableOnContentEditable?: boolean // Enable hotkeys on tags with contentEditable props. (Default: false)
  preventDefault?: Trigger // Prevent default browser behavior? (Default: false)
  displayInOverview?: boolean,
  splitKey?: string,
}
export type ActiveHotkey = {
  options?: Options & { scope: ScopeName },
}
export type ActiveHotkeys = Immutable.Map<`${ScopeName}.${string}`, ActiveHotkey>
export type HotkeyCollection = {
  title: string,
  description: string,
  actions: Record<string, { keys: string | Array<string>, description: string, displayKeys?: string }>,
}
export type HotkeyCollections = Record<ScopeName, HotkeyCollection>

type HotkeysContextType = {
  enabledScopes: Array<ScopeName | DefaultScopeName>,
  hotKeysCollections: HotkeyCollections,
  activeHotkeys: ActiveHotkeys,
  addActiveHotkey: (props: { scope: ScopeName, actionKey: string, options: Options & { scope: ScopeName } }) => void,
  removeActiveHotkey: (props: { scope: ScopeName, actionKey: string }) => void,
  showHotkeysModal: boolean,
  setShowHotkeysModal: React.Dispatch<React.SetStateAction<boolean>>
}

const HotkeysContext = React.createContext<HotkeysContextType | undefined>(undefined);

export default singleton('contexts.HotkeysContext', () => HotkeysContext);
