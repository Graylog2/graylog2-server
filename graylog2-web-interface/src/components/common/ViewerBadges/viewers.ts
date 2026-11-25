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
import type { Viewer } from './types';

const firstLetter = (word: string) => Array.from(word)[0] ?? '';

const words = (text: string | undefined) => (text ?? '').trim().split(/\s+/).filter(Boolean);

export const displayName = ({ username, full_name }: Viewer) => (full_name?.trim() ? full_name.trim() : username);

export const initials = (viewer: Viewer) => {
  const nameWords = words(viewer.full_name);

  if (nameWords.length > 1) {
    return (firstLetter(nameWords[0]) + firstLetter(nameWords[nameWords.length - 1])).toUpperCase();
  }

  return Array.from(nameWords[0] ?? viewer.username)
    .slice(0, 2)
    .join('')
    .toUpperCase();
};

export const viewerLabel = (viewer: Viewer, isCurrentUser: boolean) => {
  const name = displayName(viewer);

  if (isCurrentUser) {
    return `You (${name})`;
  }

  return name === viewer.username ? name : `${name} (${viewer.username})`;
};

export const currentUserFirst = (viewers: Array<Viewer>, currentUserId: string | undefined) => [
  ...viewers.filter(({ id }) => id === currentUserId),
  ...viewers.filter(({ id }) => id !== currentUserId),
];
