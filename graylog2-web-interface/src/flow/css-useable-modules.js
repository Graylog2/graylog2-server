// @flow strict
type CSSModule = {
  [key: string]: string,
  use: () => void,
  unuse: () => void,
  locals: { [key: string]: string },
};
const emptyCSSModule: CSSModule = {};
export default emptyCSSModule;
