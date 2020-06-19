// @flow strict

import 'opensans-npm-webfont/open_sans.css';
import 'opensans-npm-webfont/open_sans_italic.css';
import 'opensans-npm-webfont/open_sans_bold.css';
import '@openfonts/roboto-mono_latin/index.css';

export type Fonts = {
  family: {
    body: string,
    monospace: string,
  },
  size: {
    root: string,
    body: string,
    huge: string,
    large: string,
    small: string,
    tiny: string,
    h1: string,
    h2: string,
    h3: string,
    h4: string,
    h5: string,
    h6: string,
  },
};

const family = {
  body: '"Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif',
  monospace: '"Roboto Mono", Menlo, Monaco, Consolas, "Courier New", monospace',
};

/* Scaled 1.125 Major Second - https://type-scale.com/ */
const size = {
  root: '87.5%', /* 14px */
  body: '1em',
  huge: '2.027em',
  large: '1.125em',
  small: '0.889em',
  tiny: '0.79em',
  h1: '1.802em',
  h2: '1.602em',
  h3: '1.424em',
  h4: '1.266em',
  h5: '1.125em',
  h6: '1em',
};

const fonts: Fonts = {
  family,
  size,
};

export default fonts;
