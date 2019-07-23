// @flow strict

/* eslint-disable no-undef */
// $FlowFixMe: Overriding type
const asMock = (fn): JestMockFn<*, *> => fn;
/* eslint-enable no-undef */

export default asMock;
