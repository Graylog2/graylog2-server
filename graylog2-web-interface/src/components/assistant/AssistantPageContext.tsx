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
import { useCallback, useContext, useEffect, useId, useState, useSyncExternalStore } from 'react';

import useLocation from 'routing/useLocation';
import { currentPathnameWithoutPrefix, stripPrefixFromPathname } from 'util/URLUtils';

import type { Page, PageContext, PageTools, Panel } from './types';

type ContextType = {
  setPage: (key: string, page: Page | undefined) => void;
  setPanel: (key: string, panel: Panel | undefined) => void;
  setTools: (key: string, tools: PageTools | undefined) => void;
  getSnapshot: () => PageContext;
  getTools: () => PageTools;
  // Called when the page, its panels, its route or its title change.
  subscribe: (listener: () => void) => () => void;
};

const AssistantPageContext = React.createContext<ContextType | undefined>(undefined);

type Store = ContextType & { setRoute: (route: string) => void; notify: () => void };

const createStore = (initialRoute: string): Store => {
  const pages = new Map<string, Page>();
  const panels = new Map<string, Panel>();
  const tools = new Map<string, PageTools>();
  const listeners = new Set<() => void>();
  let route = initialRoute;

  const notify = () => listeners.forEach((listener) => listener());

  const set =
    <T,>(entries: Map<string, T>, notifies: boolean) =>
    (key: string, entry: T | undefined) => {
      if (entry === undefined) {
        entries.delete(key);
      } else {
        entries.set(key, entry);
      }

      if (notifies) {
        notify();
      }
    };

  return {
    setPage: set(pages, true),
    setPanel: set(panels, true),
    setTools: set(tools, false),
    setRoute: (newRoute: string) => {
      route = newRoute;
      notify();
    },
    notify,
    subscribe: (listener: () => void) => {
      listeners.add(listener);

      return () => {
        listeners.delete(listener);
      };
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

// Pages publish what they show while they are mounted; the Assistant reads it when a question is sent. Components
// that show something about the current page follow it with `useAssistantPageSelector`.
export const AssistantPageContextProvider = ({ children }: React.PropsWithChildren) => {
  const { pathname } = useLocation();
  // The route without the path prefix, which navigating within Graylog adds.
  const route = stripPrefixFromPathname(pathname);
  const [store] = useState(() => createStore(route));

  useEffect(() => {
    store.setRoute(route);
  }, [route, store]);

  // Pages set their title in effects, often only once their data is loaded.
  useEffect(() => {
    const observer = new MutationObserver(store.notify);
    observer.observe(document.head, { childList: true, subtree: true, characterData: true });

    return () => observer.disconnect();
  }, [store]);

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
        route: currentPathnameWithoutPrefix(),
        title: document.title,
        page: { type: 'generic' },
        panels: [],
      },
    [context],
  );
  const getTools = useCallback((): PageTools => context?.getTools() ?? {}, [context]);

  return { getSnapshot, getTools };
};

const emptySubscription = () => () => {};

/**
 * Follows the current page: re-renders when what `select` returns changes, e.g. after moving to another page, once it
 * has published its context and title. Return a primitive, as it is compared with `Object.is`.
 */
export const useAssistantPageSelector = <T extends string | number | boolean | undefined>(
  select: (page: PageContext) => T,
) => {
  const context = useContext(AssistantPageContext);
  const { getSnapshot } = useAssistantPageSnapshot();

  return useSyncExternalStore(context?.subscribe ?? emptySubscription, () => select(getSnapshot()));
};
