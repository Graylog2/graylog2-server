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
import * as React from 'react';
import PropTypes from 'prop-types';
import { last, sortBy } from 'lodash';
import { withTheme, DefaultTheme } from 'styled-components';

import StringUtils from 'util/StringUtils';
import { DEFAULT_HIGHLIGHT_COLOR } from 'views/Constants';
import { isFunction } from 'views/logic/aggregationbuilder/Series';

import formatNumber from './FormatNumber';
import isNumeric from './IsNumeric';

export type HighlightRange = {
  start: number,
  length: number,
};

type Ranges = { [key: string]: Array<HighlightRange> };

const highlight = (value: any, idx: number, style = {}) => <span key={`highlight-${idx}`} style={style}>{value}</span>;

type Props = {
  color: string,
  field: string,
  value?: any,
  highlightRanges: Ranges,
  theme: DefaultTheme,
};

function highlightCompleteValue(ranges: Array<HighlightRange>, value) {
  if (ranges.length !== 1) {
    return false;
  }

  const { start, length } = ranges[0];
  const stringifiedValue = StringUtils.stringify(value);

  return start === 0 && length === stringifiedValue.length;
}

const shouldBeFormatted = (field, value) => isFunction(field) && isNumeric(value);

const PossiblyHighlight = ({ color = DEFAULT_HIGHLIGHT_COLOR, field, value, highlightRanges = {}, theme }: Props) => {
  if (value === undefined || value === null) {
    return '';
  }

  if (!highlightRanges || !highlightRanges[field]) {
    return shouldBeFormatted(field, value)
      ? formatNumber(value)
      : value;
  }

  const style = {
    backgroundColor: color,
    color: theme.utils.contrastingColor(color),
    padding: '0 1px',
  };

  if (highlightCompleteValue(highlightRanges[field], value)) {
    const formattedValue = shouldBeFormatted(field, value)
      ? formatNumber(value)
      : value;

    return highlight(formattedValue, 0, style);
  }

  // Ensure the field is a string for later processing
  const origValue = StringUtils.stringify(value);

  const ranges = sortBy(highlightRanges[field], (r) => r.start);
  const subst = (s, l) => origValue.substring(s, s + l);
  const rest = (pos) => origValue.substring(pos, origValue.length);

  const highlights = ranges
    .filter(({ start }) => (start >= 0))
    .filter(({ length }) => (length >= 0))
    .reduce<[HighlightRange[], number]>(([acc, i], cur, idx) => [
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

export default withTheme(PossiblyHighlight);
