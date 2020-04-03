// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { last, sortBy } from 'lodash';

import StringUtils from 'util/StringUtils';
import { DEFAULT_HIGHLIGHT_COLOR } from 'views/Constants';

export type HighlightRange = {|
  start: number,
  length: number,
|};

type Ranges = { [string]: Array<HighlightRange> };

const highlight = (value: any, idx: number, style = {}) => <span key={`highlight-${idx}`} style={style}>{value}</span>;

type Props = {
  color: string,
  field: string,
  value?: any,
  highlightRanges: Ranges,
};

const PossiblyHighlight = ({ color = DEFAULT_HIGHLIGHT_COLOR, field, value, highlightRanges = {} }: Props) => {
  if (value === undefined || value == null) {
    return '';
  }
  if (!highlightRanges || !highlightRanges[field]) {
    return value;
  }
  const style = {
    backgroundColor: color,
  };

  // Ensure the field is a string for later processing
  const origValue = StringUtils.stringify(value);

  const ranges = sortBy(highlightRanges[field], (r) => r.start);
  const subst = (s, l) => origValue.substring(s, s + l);
  const rest = (pos) => origValue.substring(pos, origValue.length);

  const highlights = ranges
    .filter(({ start }) => (start >= 0))
    .filter(({ length }) => (length >= 0))
    .reduce(([acc, i], cur, idx) => [
      [...acc,
        subst(i, Math.max(0, cur.start - i)), // non-highlighted string before this range
        highlight(subst(Math.max(cur.start, i), Math.max(0, cur.length - Math.max(0, i - cur.start))), idx, style), // highlighted string in range
      ],
      cur.start + cur.length,
    ], [[], 0])[0];

  const lastRange = last(sortBy(ranges, (r) => r.start + r.length));
  highlights.push(rest(lastRange.start + lastRange.length));

  return <>{highlights}</>;
};

PossiblyHighlight.propTypes = {
  color: PropTypes.string,
  field: PropTypes.string.isRequired,
  value: PropTypes.any,
  highlightRanges: PropTypes.object,
};

PossiblyHighlight.defaultProps = {
  color: DEFAULT_HIGHLIGHT_COLOR,
  highlightRanges: {},
  value: undefined,
};

export default PossiblyHighlight;
