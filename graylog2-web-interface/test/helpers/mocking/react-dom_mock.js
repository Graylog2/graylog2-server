/* https://github.com/facebook/react/issues/7371
 *
 * findDomNode with refs is not supported by the react-test-renderer.
 * So we need to mock the findDOMNode function for TableList respectievly
 * for its child component TypeAheadDataFilter.
 */
jest.mock('react-dom', () => ({
  findDOMNode: () => ({}),
}));

