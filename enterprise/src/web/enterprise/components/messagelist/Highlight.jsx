// @flow strict
import * as React from 'react';
import { get, last, sortBy } from 'lodash';

// $FlowFixMe: imports from core need to be fixed in flow
import StringUtils from 'util/StringUtils';

import { AdditionalContext } from 'enterprise/logic/ActionContext';

type Props = {
  field: string,
  value: any,
};

type HighlightRange = {|
  start: number,
  length: number,
|};

type Ranges = { [string]: Array<HighlightRange> };

const highlight = (value, idx) => <span key={`highlight-${idx}`} className="result-highlight-colored">{value}</span>;

const possiblyHighlight = (field: string, value: any, highlightRanges: Ranges = {}, truncate: boolean = false) => {
  if (value === undefined) {
    return '';
  }
  // Ensure the field is a string for later processing
  const fullStringOrigValue = StringUtils.stringify(value);

  // Truncate the field to 2048 characters if requested. This is for performance reasons to avoid hogging the CPU.
  // It's not optimal, more like a workaround to at least being able to show the page...
  const origValue = truncate ? fullStringOrigValue.slice(0, 2048) : fullStringOrigValue;

  if (highlightRanges && highlightRanges[field]) {
    const ranges = sortBy(highlightRanges[field], r => r.start);
    const subst = (s, l) => origValue.substring(s, s + l);
    const rest = pos => origValue.substring(pos, origValue.length);

    const highlights = ranges
      .filter(({ start }) => (start >= 0))
      .filter(({ length }) => (length >= 0))
      .reduce(([acc, i], cur, idx) => [
        [...acc,
          subst(i, Math.max(0, cur.start - i)), // non-highlighted string before this range
          highlight(subst(Math.max(cur.start, i), Math.max(0, cur.length - Math.max(0, i - cur.start))), idx), // highlighted string in range
        ],
        cur.start + cur.length,
      ], [[], 0])[0];

    const lastRange = last(sortBy(ranges, r => r.start + r.length));
    highlights.push(rest(lastRange.start + lastRange.length));

    return <React.Fragment>{highlights}</React.Fragment>;
  }
  return String(origValue);
};

const Highlight = ({ field, value }: Props) => (
  <AdditionalContext.Consumer>
    {({ message }) => possiblyHighlight(field, value, get(message, 'highlight_ranges', {}), false)}
  </AdditionalContext.Consumer>
);

Highlight.propTypes = {};

export default Highlight;
