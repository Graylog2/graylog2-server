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
 * React Router v7 future flags, set explicitly. `react-router-dom@6` emits a
 * deprecation warning for every v7 future flag that is left unset (`undefined`),
 * and `console-warnings-fail-tests` turns those warnings into test failures.
 * Setting a flag to `false` is still "set", so it silences the warning while
 * keeping the v6 behavior.
 *
 * `v7_startTransition` is deliberately `false` in tests: with it enabled,
 * react-router wraps every navigation (incl. query-param updates via
 * `useQueryParam`) in `React.startTransition`, marking it a low-priority update.
 * Under the parallel test runner's CPU contention those transitions get starved
 * and the resulting UI does not render within a test's wait window, producing
 * flaky, worker-assignment-dependent failures. Tests therefore keep navigation
 * synchronous; production still opts into the v7 behavior in `AppRouter`.
 *
 * Use these constants wherever a `Router` / `RouterProvider` / `createMemoryRouter`
 * is mounted in tests, instead of repeating the flag literals.
 */

// Flags accepted by component routers (`<MemoryRouter>`, `<Router>`).
export const memoryRouterFuture = {
  v7_startTransition: false,
  v7_relativeSplatPath: true,
} as const;

// Flag accepted by the `future` prop of `<RouterProvider>` (data routers).
export const routerProviderFuture = {
  v7_startTransition: false,
} as const;

// Flags accepted by data routers (`createMemoryRouter` / `createBrowserRouter`).
export const dataRouterFuture = {
  v7_relativeSplatPath: true,
  v7_fetcherPersist: true,
  v7_normalizeFormMethod: true,
  v7_partialHydration: true,
  v7_skipActionErrorRevalidation: true,
} as const;
