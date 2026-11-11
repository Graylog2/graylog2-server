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
import { useCallback, useContext, useEffect, useId, useState } from 'react';

import useLocation from 'routing/useLocation';

import type { Page, PageContext, PageTools, Panel } from './types';

type ContextType = {
  setPage: (key: string, page: Page | undefined) => void;
  setPanel: (key: string, panel: Panel | undefined) => void;
  setTools: (key: string, tools: PageTools | undefined) => void;
  getSnapshot: () => PageContext;
  getTools: () => PageTools;
};

const AssistantPageContext = React.createContext<ContextType | undefined>(undefined);

const createStore = (initialRoute: string): ContextType & { setRoute: (route: string) => void } => {
  const pages = new Map<string, Page>();
  const panels = new Map<string, Panel>();
  const tools = new Map<string, PageTools>();
  let route = initialRoute;

  const set =
    <T,>(entries: Map<string, T>) =>
    (key: string, entry: T | undefined) => {
      if (entry === undefined) {
        entries.delete(key);
      } else {
        entries.set(key, entry);
      }
    };

  return {
    setPage: set(pages),
    setPanel: set(panels),
    setTools: set(tools),
    setRoute: (newRoute: string) => {
      route = newRoute;
    },
    // The page registered last wins, e.g. a nested route loaded after its parent; the earlier one comes back
    // when it unmounts.
    getSnapshot: () => ({
      version: 1,
      route,
      title: document.title,
      page: [...pages.values()].at(-1) ?? { type: 'generic' },
      panels: [...panels.values()],
    }),
    getTools: () => Object.assign({}, ...tools.values()),
  };
};

// Pages publish what they show while they are mounted; the Assistant reads it when a question is sent, so
// publishing never re-renders the chat.
export const AssistantPageContextProvider = ({ children }: React.PropsWithChildren) => {
  const { pathname } = useLocation();
  const [store] = useState(() => createStore(pathname));

  useEffect(() => {
    store.setRoute(pathname);
  }, [pathname, store]);

  return <AssistantPageContext.Provider value={store}>{children}</AssistantPageContext.Provider>;
};

const usePublish = <T,>(publish: ((key: string, entry: T | undefined) => void) | undefined, entry: T | undefined) => {
  const key = useId();

  useEffect(() => {
    publish?.(key, entry);
  }, [entry, key, publish]);

  useEffect(() => () => publish?.(key, undefined), [key, publish]);
};

// Publishes what the page shows while it is mounted. Pass a memoized value.
export const useAssistantPageContext = (page: Page | undefined) =>
  usePublish(useContext(AssistantPageContext)?.setPage, page);

// Publishes an open details sidebar while it is mounted. Pass a memoized value.
export const useAssistantPanelContext = (panel: Panel | undefined) =>
  usePublish(useContext(AssistantPageContext)?.setPanel, panel);

// Registers the page tools the Assistant may ask this page to run. Pass a memoized value.
export const useAssistantPageTools = (pageTools: PageTools | undefined) =>
  usePublish(useContext(AssistantPageContext)?.setTools, pageTools);

// For the Assistant's chat: what the page shows, and its tools, at the time of the call.
export const useAssistantPageSnapshot = () => {
  const context = useContext(AssistantPageContext);
  const getSnapshot = useCallback(
    (): PageContext =>
      context?.getSnapshot() ?? {
        version: 1,
        route: window.location.pathname,
        title: document.title,
        page: { type: 'generic' },
        panels: [],
      },
    [context],
  );
  const getTools = useCallback((): PageTools => context?.getTools() ?? {}, [context]);

  return { getSnapshot, getTools };
};
