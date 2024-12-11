import moment from 'moment';
import trim from 'lodash/trim';

import { extractRangeFromString } from 'components/common/EntityFilters/helpers/timeRange';
import { adjustFormat } from 'util/DateTime';
import type { TimeRange, RelativeTimeRange } from 'views/logic/queries/Query';

const allTimesRange: RelativeTimeRange = { type: 'relative', range: 0 };

const isNullOrBlank = (s: string | undefined) => {
  if (!s) {
    return true;
  }

  return trim(s) === '';
};

const parseTimerangeFilter = (timestamp: string | undefined): TimeRange => {
  if (!timestamp) {
    return allTimesRange;
  }

  const [from, to] = extractRangeFromString(timestamp);

  if (!from && !to) {
    return allTimesRange;
  }

  return {
    type: 'absolute',
    from: isNullOrBlank(from) ? adjustFormat(moment(0).utc(), 'internal') : from,
    to: isNullOrBlank(to) ? adjustFormat(moment().utc(), 'internal') : to,
  };
};

export default parseTimerangeFilter;
