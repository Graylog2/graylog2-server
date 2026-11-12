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
import { useState } from 'react';

import Store from 'logic/local-storage/Store';

export type SidebarBounds = { initial: number; min: number; max: number };

export const clampWidth = (width: number, { min, max }: SidebarBounds) =>
  Math.min(max, Math.max(min, Math.round(width)));

// Storage can be unavailable, e.g. in private windows; the sidebar then starts at its initial width.
const storedWidth = (storageKey: string, bounds: SidebarBounds) => {
  try {
    const width = Store.get<number>(storageKey);

    return typeof width === 'number' && Number.isFinite(width) ? clampWidth(width, bounds) : bounds.initial;
  } catch {
    return bounds.initial;
  }
};

/**
 * The width of a resizable sidebar, kept in the browser's local storage under `storageKey`.
 */
const useSidebarWidth = (storageKey: string, bounds: SidebarBounds) => {
  const [width, setWidthState] = useState(() => storedWidth(storageKey, bounds));

  const setWidth = (next: number) => {
    const clamped = clampWidth(next, bounds);
    setWidthState(clamped);

    try {
      Store.set(storageKey, clamped);
    } catch {
      // The width then only lasts until the page is reloaded.
    }
  };

  return [width, setWidth] as const;
};

export default useSidebarWidth;
