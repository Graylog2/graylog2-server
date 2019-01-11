// @flow
type CSSModule = {
  [key: string]: string,
  use: () => void,
  unuse: () => void,
};
// $FlowFixMe: Missing attributes on purpose.
const emptyCSSModule: CSSModule = {};
export default emptyCSSModule;
