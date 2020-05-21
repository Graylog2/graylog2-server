// @flow strict
import teinte from './variants/teinte';

export type Colors = {
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
    lighter: {
      danger: string,
      default: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
    lightest: {
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
    darker: {
      danger: string,
      default: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
    darkest: {
      danger: string,
      default: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
  },
};

const colors = {
  teinte,
};

export default colors;
export {
  teinte,
};
