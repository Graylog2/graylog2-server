// @flow
type CSSModule = {
  [key: string]: string,
  use: () => void,
  unuse: () => void,
  locals: { [key: string]: string },
};
// $FlowFixMe: Missing attributes on purpose.
const emptyCSSModule: CSSModule = {};
export default emptyCSSModule;
