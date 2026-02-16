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
import type { QualifiedUrl } from 'routing/Routes';

type SearchResultItemBase = {
  key?: string;
  type: string;
  title: string;
  score?: number;
  last_opened?: boolean;
  favorite?: boolean;
};

type SearchResultItemLink = {
  link: QualifiedUrl<string>;
};

type SearchResultItemExternalLink = {
  externalLink: string;
};

type ActionArguments = {
  logout: () => void;
  showHotkeysModal: () => void;
  toggleScratchpad: () => void;
  toggleThemeMode: () => void;
};
type SearchResultItemAction = {
  action: (args: ActionArguments) => void;
};

export type SearchResultItem = SearchResultItemBase &
  (SearchResultItemLink | SearchResultItemExternalLink | SearchResultItemAction);
