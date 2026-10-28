/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { ColorVariant, ColorScheme, Utils } from '../types';

export type TColorVariants = Partial<Record<ColorVariant, string>>;
export type TColorVariantShades = 'dark' | 'darker' | 'darkest' | 'light' | 'lighter' | 'lightest'
export type DisabledColor = {
  background: string,
  color: string,
}
export type DisabledColors = Record<ColorVariant, DisabledColor>;
export type ContrastColors = Record<ColorVariant, string>;
export type ButtonColors = Record<ColorVariant, { background: string, color: string }>;
export type AlertColors = Record<ColorVariant, { background: string, border: string }>;

type BadgeVariantStyles = {
  light: {
    text: string,
    background: string
  },
  filled: {
    text: string,
    background: string
  }
}

type ButtonVariantStyles = {
  light: {
    text: string,
    background: string,
    backgroundHover: string,
  },
  filled: {
    text: string,
    background: string,
    backgroundHover: string,
  },
  outlined: {
    text: string,
    border: string,
    backgroundHover: string,
  },
  subtle: {
    text: string,
    backgroundHover: string,
  }
}

export type TColors = {
  alerts: AlertColors,
  button: ButtonColors,
  brand: {
    primary: string,
    secondary: string,
    tertiary: string,
    logo: string,
    concrete: string,
  },
  contrast: ContrastColors,
  disabled: DisabledColors,
  global: {
    background: string,
    contentBackground: string,
    link: string,
    linkHover: string,
    navigationBackground: string,
    navigationBoxShadow: string,
    textAlt: string,
  },
  gray: {
    '10': string,
    '20': string,
    '30': string,
    '40': string,
    '50': string,
    '60': string,
    '70': string,
    '80': string,
    '90': string,
    '100': string,
  },
  input: {
    background: string,
    backgroundDisabled: string,
    border: string,
    borderFocus: string,
    boxShadow: string,
    color: string,
    colorDisabled: string,
    placeholder: string,
  },
  pagination: {
    active: {
      color: string,
      border: string,
      background: string,
    }
  },
  table: {
    variant: TColorVariants,
    variantHover: TColorVariants,
    head: {
      background: string,
    },
    row: {
      background: string,
      backgroundExpanded: string,
      backgroundHover: string,
      backgroundStriped: string,
      divider: string,
    },
    column: {
      divider: string,
    },
    highlightBackground: string,
    highlightBorder: string,
    highlightText: string,
  },
  variant: TColorVariants & Record<TColorVariantShades, TColorVariants>,

  // new
  background: {
    body: string,
    content: string,
    secondaryNav: string,
  },
  badges: {
    dotBorder: string,
    primary: BadgeVariantStyles,
    danger: BadgeVariantStyles,
    success: BadgeVariantStyles,
    warning: BadgeVariantStyles,
    gray: BadgeVariantStyles,
  },
  buttons: {
    disabledBackground: string,
    blue: ButtonVariantStyles,
    red: ButtonVariantStyles,
    gray: ButtonVariantStyles,
  }
  cards: {
    background: string,
    border: string,
  },
  charts: {
    containerBackground: string,
    containerBorder: string,
    highlight: string,
    lines: string,
    cyan: {
      markerColor: string,
      textColor: string,
      fill: string,
    }
  },
  icons: {
    icon: string,
    active: string,
    disabled: string,
  },
  link: {
    default: string,
    hover: string,
    disabled: string,
  },
  menu: {
    default: {
      color: string,
      hover: string,
      activeColor: string,
      activeBackground: string,
    },
    red: {
      color: {
        color: string,
        active: string,
      }
    }
  },
  misc: {
    divider: string,
    logoColor: string,
    stripedBackground: string,
  },
  newsCards: {
    background: string
  },
  notification: {
    background: string,
    border: string,
    blue: string,
    red: string,
    green: string,
    progressBackground: string
  }
  section: {
    filled: {
      border: string,
      background: string,
    }
  },
  severity: {
    high: string,
    low: string,
    medium: string,
    success: string,
  },
  tabs: {
    hover: string,
    active: string,
  }
  text: {
    primary: string,
    secondary: string,
    disabled: string,
    severity: {
      success: string,
      danger: string,
    }
  },
}

export type TBreakpoint = {
  xs: string;
  sm: string;
  md: string;
  lg: string;
  xl: string;
};

export type TBreakpoints = {
  min: TBreakpoint,
  max: TBreakpoint,
  px: {
    min: Record<keyof TBreakpoint, number>,
    max: Record<keyof TBreakpoint, number>,
  }
};

export type TFonts = {
  family: {
    body: string,
    monospace: string,
    navigation: string,
  },
  lineHeight: { body: string },
  size: {
    root: string,
    body: string,
    huge: string,
    extraLarge: string,
    large: string,
    small: string,
    tiny: string,
    navigation: string,
    h1: string,
    h2: string,
    h3: string,
    h4: string,
    h5: string,
    h6: string,
  },
};

export type TSpacingSizes = 'xxs' | 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl';
export type TSpacing = Record<TSpacingSizes, string> & { px: Record<TSpacingSizes, number> };

export interface StyledComponentsTheme {
  breakpoints: TBreakpoints,
  colors: TColors,
  fonts: TFonts,
  mode: ColorScheme
  spacings: TSpacing,
  utils: Utils,
  components: { aceEditor: string },
}
