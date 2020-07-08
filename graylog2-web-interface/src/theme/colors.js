// @flow strict
import teinte from './variants/teinte';

export type Colors = {
  brand: {
    primary: string,
    secondary: string,
    tertiary: string,
  },
  global: {
    background: string,
    contentBackground: string,
    inputBackground: string,
    link: string,
    linkHover: string,
    textAlt: string,
    textDefault: string,
  },
  gray: {
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
  input: {
    background: string,
    backgroundDisabled: string,
    backgroundFocus: string,
    border: string,
    boxShadow: string,
    color: string,
    colorDisabled: string,
  },
  table: {
    background: string,
    backgroundAlt: string,
    backgroundHover: string,
    variant: {
      active: string,
      danger: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
    variantHover: {
      active: string,
      danger: string,
      info: string,
      primary: string,
      success: string,
      warning: string,
    },
  },
  variant: {
    danger: string,
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
    default: string,
    info: string,
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
    primary: string,
    success: string,
    warning: string,
  },
};

const colors = {
  ...teinte,
};

export default colors;
export {
  teinte,
};
