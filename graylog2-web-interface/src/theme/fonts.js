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
    large: string,
    small: string,
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

/* Scaled 1.20 Minor Third - https://type-scale.com/ */
const size = {
  root: '87.5%', /* 14px */
  body: '1em',
  large: '1.2em',
  small: '0.833em',
  h1: '2.488em',
  h2: '2.074em',
  h3: '1.728em',
  h4: '1.44em',
  h5: '1.2em',
  h6: '1em',
};

const fonts: Fonts = {
  family,
  size,
};

export default fonts;
