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

/*
 * React Router v7 future flags, opted into explicitly. `react-router-dom@6`
 * emits a deprecation warning for every v7 future flag that is not set, and
 * `console-warnings-fail-tests` turns those warnings into test failures.
 *
 * Use these constants wherever a `Router` / `RouterProvider` / `createMemoryRouter`
 * is mounted in tests, instead of repeating the flag literals.
 */

// Flags accepted by component routers (`<MemoryRouter>`, `<Router>`).
export const memoryRouterFuture = {
  v7_startTransition: true,
  v7_relativeSplatPath: true,
} as const;

// Flags accepted by data routers (`createMemoryRouter` / `createBrowserRouter`).
export const dataRouterFuture = {
  v7_relativeSplatPath: true,
  v7_fetcherPersist: true,
  v7_normalizeFormMethod: true,
  v7_partialHydration: true,
  v7_skipActionErrorRevalidation: true,
} as const;
