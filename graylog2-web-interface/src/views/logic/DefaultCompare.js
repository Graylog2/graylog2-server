// @flow strict
/*
 * Default (string) comparison function used for sorting lists/arrays.
 *
 * This module should be used whenever a consistent, high-performance comparison function for sorting is required. It is
 * especially useful when used instead of `naturalSort` to avoid its excessive usage of regular expressions, but with the
 * `{ numeric: true }` option, a subset (and probably the most desired one) of its functionality can be replaced while
 * being magnitudes faster.
 *
 * The `compare` function returns the comparison function, so it needs to be called before its result is passed to e.g.
 * `sort()`. This is because it instantiates a `Intl.Collator` instance, which is reused in subsequent calls.
 * If only the default options (non-numeric, case-insensitive) are required, `defaultCompare` can be used which does not
 * require to be called before and is therefore a drop-in replacement for `naturalSort`.
 */

/*
 * This generator function can be used to instantiate a comparison method with non-standard options. The usable options
 * are listed here: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Collator -> Syntax
 * -> Parameters -> options
 *
 * The `undefined` parameter when instantiating triggers using the runtime's default locales (i.e. the locales supplied
 * by the browser).
 */

export const compare = (options: * = {}) => new Intl.Collator(undefined, options).compare;

/*
 * This is the default comparison function. It compares non-numerically and case-insensitive. It can be used as a drop-in
 * replacement for `naturalSort` and is shared across its consumers. If the default options do not match, `compare` must
 * be used instead.
 */
export const defaultCompare: (any, any) => number = compare();
