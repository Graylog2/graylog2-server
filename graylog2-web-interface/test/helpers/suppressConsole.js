// @flow strict

/**
 * This helper function allows to suppress console errors which would lead to test failures in some, very rare cases.
 *
 * The only accepted situation at this moment is for testing react error boundaries, which inevitably log the error which
 * was thrown to the console. Testing an error boundary currently requires using this function.
 *
 * @param {function} fn - the function which should be called after disabling `console.error` and before restoring it.
 */
const suppressConsole = (fn: () => mixed) => {
  /* eslint-disable no-console */
  const originalConsoleError = console.error;
  // $FlowFixMe: We explicitly want to overwrite `error`
  console.error = () => {};

  fn();

  // $FlowFixMe: We explicitly want to overwrite `error`
  console.error = originalConsoleError;
  /* eslint-enable no-console */
};

export default suppressConsole;
