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
  },
};

const family = {
  body: '"Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif',
  monospace: '"Roboto Mono", Menlo, Monaco, Consolas, "Courier New", monospace',
};

const size = {
  root: '16px',
  body: '0.8em',
  bodyLarge: '1em',
  bodySmall: '0.65em',
  h1: '2em',
  h2: '1.8em',
  h3: '1.5em',
  h4: '1.25em',
  h5: '1em',
  h6: '0.8em',
};

const fonts: Fonts = {
  family,
  size,
};

export default fonts;
