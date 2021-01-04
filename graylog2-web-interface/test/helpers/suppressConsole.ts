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

/**
 * This helper function allows to suppress console errors which would lead to test failures in some, very rare cases.
 *
 * The only accepted situation at this moment is for testing react error boundaries, which inevitably log the error which
 * was thrown to the console. Testing an error boundary currently requires using this function.
 *
 * @param {function} fn - the function which should be called after disabling `console.error` and before restoring it.
 */
const suppressConsole = (fn: () => void) => {
  /* eslint-disable no-console */
  const originalConsoleError = console.error;
  console.error = () => {};

  fn();

  console.error = originalConsoleError;
  /* eslint-enable no-console */
};

export default suppressConsole;
