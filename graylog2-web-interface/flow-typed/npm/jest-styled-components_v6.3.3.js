/**
 * Flowtype definitions for jest-styled-components
 */

/* eslint-disable no-undef */
declare interface jest$AsymmetricMatcher {
  $$typeof: Symbol;
  sample?: string | RegExp | { [key: string]: any } | Array<any> | Function;
}

declare type jest$Value =
  | string
  | number
  | RegExp
  | jest$AsymmetricMatcher
  | void;

declare interface jest$Options {
  media?: string;
  modifier?: string;
  supports?: string;
}

declare interface jest$Matchers<R, T> {
  toHaveStyleRule(
    property: string,
    value?: jest$Value,
    options?: jest$Options
  ): R;
}

declare module 'jest-styled-components' {
  declare module.exports: any;
}
