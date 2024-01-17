import { useCallback, useState } from 'react';

import { SearchQueryStrings } from '@graylog/server-api';
import type { SearchBarFormValues } from 'views/Constants';
import useUserDateTime from 'hooks/useUserDateTime';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import { normalizeFromSearchBarForBackend } from 'views/logic/queries/NormalizeTimeRange';
import type { TimeRange } from 'views/logic/queries/Query';

const executeWithQueryStringRecording = async <R, >(isDirty: boolean, query: string, callback: () => R) => {
  const trimmedQuery = query.trim();

  try {
    if (isDirty && !!trimmedQuery) {
      await SearchQueryStrings.queryStringUsed({ query_string: trimmedQuery });
    }
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error('Unable to record last used query string: ', error);
  }

  return callback();
};

type FormValues = {
  queryString: string,
  timerange: TimeRange,
}

const useSearchBarSubmit = (initialValues: FormValues, onSubmit: (v: FormValues) => Promise<unknown>) => {
  const { userTimezone } = useUserDateTime();
  const [enableReinitialize, setEnableReinitialize] = useState(true);
  const _onSubmit = useCallback((values: SearchBarFormValues) => {
    setEnableReinitialize(false);
    const { queryString, timerange, ...rest } = values;

    return executeWithQueryStringRecording(
      queryString !== initialValues?.queryString,
      queryString,
      () => onSubmit({
        queryString,
        timerange: isNoTimeRangeOverride(timerange) ? undefined : normalizeFromSearchBarForBackend(timerange, userTimezone),
        ...rest,
      }).finally(() => setEnableReinitialize(true)),
    );
  }, [initialValues?.queryString, onSubmit, userTimezone]);

  return { enableReinitialize, onSubmit: _onSubmit };
};

export default useSearchBarSubmit;
