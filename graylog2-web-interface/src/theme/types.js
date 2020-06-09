// @flow strict

import { type Fonts } from './fonts';
import { type Utils } from './utils';

export type ThemeInterface = {
  color: {
    brand: {
      primary: string,
      secondary: string,
      tertiary: string,
    },
    global: {
      textDefault: string,
      textAlt: string,
      background: string,
      contentBackground: string,
      link: string,
      linkHover: string,
    },
    gray: {
      "0": string,
      "10": string,
      "20": string,
      "30": string,
      "40": string,
      "50": string,
      "60": string,
      "70": string,
      "80": string,
      "90": string,
      "100": string,
    },
    variant: {
      danger: string,
      default: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
      light: {
        danger: string,
        default: string,
        info: string,
        primary: string,
        success: string,
        warning: string,
      },
      dark: {
        danger: string,
        default: string,
        info: string,
        primary: string,
        success: string,
        warning: string,
      },
    },
  },
  fonts: Fonts,
  utils: Utils,
};
