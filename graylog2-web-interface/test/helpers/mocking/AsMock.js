// @flow strict

// $FlowFixMe: Overriding type
const asMock = (fn): JestMockFn<*, *> => fn;

export default asMock;
