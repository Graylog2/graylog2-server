// flow-typed signature: a075da302542e1c92aeb3dd49cbae6af
// flow-typed version: 7c517f55da/qs_v6.9.x/flow_>=v0.104.x

declare module "qs" {
  declare type Charset =
    | 'iso-8859-1'
    | 'utf-8'

  declare type ParseOptions = {|
    allowDots?: boolean,
    allowPrototypes?: boolean,
    arrayLimit?: number,
    charset?: Charset,
    charsetSentinel?: boolean,
    comma?: boolean,
    decoder?: string => mixed,
    delimiter?: string,
    depth?: number,
    ignoreQueryPrefix?: boolean,
    interpretNumericEntities?: boolean,
    parameterLimit?: number,
    parseArrays?: boolean,
    plainObjects?: boolean,
    strictNullHandling?: boolean,
  |};

  declare type ArrayFormat = "brackets" | "comma" | "indices" | "repeat";

  declare type FilterFunction = (prefix: string, value: mixed) => mixed;
  declare type FilterArray = Array<string | number>;
  declare type Filter = FilterArray | FilterFunction;

  declare type StringifyOptions = {|
    addQueryPrefix?: boolean,
    allowDots?: boolean,
    arrayFormat?: ArrayFormat,
    charset?: Charset,
    charsetSentinel?: boolean,
    delimiter?: string,
    encode?: boolean,
    encodeValuesOnly?: boolean,
    encoder?: mixed => string,
    filter?: Filter,
    format?: string,
    indices?: boolean,
    serializeDate?: Function,
    skipNulls?: boolean,
    sort?: (string, string) => -1 | 0 | 1,
    strictNullHandling?: boolean,
  |};

  declare type Formatter = (any) => string;

  declare type Formats = {
    RFC1738: string,
    RFC3986: string,
    "default": string,
    formatters: {
      RFC1738: Formatter,
      RFC3986: Formatter,
      ...
    },
    ...
  };

  declare module.exports: {|
    parse(str: string, opts?: ParseOptions): { [string]: mixed, ... },
    stringify(
      obj: $ReadOnly<{ [string]: mixed, ... }> | $ReadOnlyArray<mixed>,
      opts?: StringifyOptions
    ): string,
    formats: Formats,
  |};
}
