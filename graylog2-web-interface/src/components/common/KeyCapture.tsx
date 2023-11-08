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
import React from 'react';
import type { PropsWithChildren } from 'react';

import type { HotkeysProps } from 'hooks/useHotkey';
import useHotkey from 'hooks/useHotkey';

type Props = PropsWithChildren<{
  shortcuts: Array<HotkeysProps>,
}>;

const HotkeyComponent = ({ shortcut }: { shortcut: HotkeysProps }) => {
  useHotkey(shortcut);

  return null;
};

const KeyCapture = ({ children, shortcuts } : Props) => (
  <>
    {shortcuts.map((shortcut) => <HotkeyComponent key={`${shortcut.scope}.${shortcut.actionKey}`} shortcut={shortcut} />)}
    {children}
  </>
);

KeyCapture.defaultProps = {
  children: null,
};

export default KeyCapture;
