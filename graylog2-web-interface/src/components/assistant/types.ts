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
// The page context the Assistant gets with each question. Mirrors the enterprise plugin's `PageContext` records.
export type TimeRangeJson = { type: string; [key: string]: unknown };

export type SearchPage = {
  type: 'search';
  view: unknown;
  dirty: boolean;
  active_query_id?: string;
  overrides?: { query?: string; timerange?: TimeRangeJson; parameter_values?: Record<string, unknown> };
  focused_widget_id?: string;
  // Whether the focused widget is open in its editor, whose changes are not in `view` until they are applied.
  focused_widget_editing?: boolean;
};

export type NotebookPage = {
  type: 'notebook';
  notebook_id: string;
  selected_cell_id?: string;
  selected_comment_id?: string;
};

export type EventsPage = {
  type: 'events';
  query?: string;
  timerange?: TimeRangeJson;
  filters?: Array<string>;
  selected_event_id?: string;
};

export type EntityPage = { type: 'entity'; grn: string };

export type EntityListPage = { type: 'entity_list'; entity_type: string; query?: string; selected_ids?: Array<string> };

export type GenericPage = { type: 'generic' };

export type Page = SearchPage | NotebookPage | EventsPage | EntityPage | EntityListPage | GenericPage;

export type Panel = { type: 'event'; event_id: string } | { type: 'asset'; asset_id: string };

export type PageContext = {
  version: 1;
  route: string;
  title?: string;
  page: Page;
  panels: Array<Panel>;
};

// A page the chat links to after a page tool ran, e.g. a stream it created; `path` is a route within Graylog.
export type PageToolLink = { label: string; path: string };

// What a page tool did, shown in the chat; `undo` reverts it. `result` tells the Assistant what it needs to refer to
// it later, e.g. the id of a created entity.
export type PageToolResult = { summary?: string; links?: Array<PageToolLink>; result?: string; undo?: () => unknown } | void;

// Runs a page tool the Assistant asked for, with arguments the backend checked.
export type PageToolHandler = (args: Record<string, unknown>) => PageToolResult | Promise<PageToolResult>;

export type PageTools = Record<string, PageToolHandler>;
