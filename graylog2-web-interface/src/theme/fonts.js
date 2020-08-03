// @flow strict
import PropTypes from 'prop-types';

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

export const fontsPropTypes = {
  family: PropTypes.shape({
    body: PropTypes.string.isRequired,
    monospace: PropTypes.string.isRequired,
  }).isRequired,
  size: PropTypes.shape({
    root: PropTypes.string.isRequired,
    body: PropTypes.string.isRequired,
    huge: PropTypes.string.isRequired,
    large: PropTypes.string.isRequired,
    small: PropTypes.string.isRequired,
    tiny: PropTypes.string.isRequired,
    h1: PropTypes.string.isRequired,
    h2: PropTypes.string.isRequired,
    h3: PropTypes.string.isRequired,
    h4: PropTypes.string.isRequired,
    h5: PropTypes.string.isRequired,
    h6: PropTypes.string.isRequired,
  }).isRequired,
};

const family = {
  body: '"Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif',
  monospace: '"Roboto Mono", Menlo, Monaco, Consolas, "Courier New", monospace',
};

/* Scaled 1.125 Major Second - https://type-scale.com/ */
const size = {
  root: '87.5%', /* 14px */
  body: '1rem',
  huge: '2.027rem',
  large: '1.125rem',
  small: '0.889rem',
  tiny: '0.79rem',
  h1: '1.802rem',
  h2: '1.602rem',
  h3: '1.424rem',
  h4: '1.266rem',
  h5: '1.125rem',
  h6: '1rem',
};

const fonts: Fonts = {
  family,
  size,
};

export default fonts;
