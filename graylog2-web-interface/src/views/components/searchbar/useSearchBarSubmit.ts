import { useCallback, useState } from 'react';

import { SearchQueryStrings } from '@graylog/server-api';
import type { SearchBarFormValues } from 'views/Constants';
import useUserDateTime from 'hooks/useUserDateTime';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import { normalizeFromSearchBarForBackend } from 'views/logic/queries/NormalizeTimeRange';
import type { TimeRange } from 'views/logic/queries/Query';

const recordQueryString = async (isDirty: boolean, query: string) => {
  try {
    if (isDirty && !!query) {
      await SearchQueryStrings.queryStringUsed({ query_string: query });
    }
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error('Unable to record last used query string: ', error);
  }
};

type FormValues = {
  queryString: string,
  timerange: TimeRange,
}

const _trim = (s: string | undefined) => (s === undefined ? undefined : s.trim());

const useSearchBarSubmit = (initialValues: FormValues, onSubmit: (v: FormValues) => Promise<unknown>) => {
  const { userTimezone } = useUserDateTime();
  const [enableReinitialize, setEnableReinitialize] = useState(true);
  const _onSubmit = useCallback(async (values: SearchBarFormValues) => {
    setEnableReinitialize(false);
    const { queryString, timerange, ...rest } = values;

    const trimmedQueryString = _trim(queryString);
    await recordQueryString(trimmedQueryString !== _trim(initialValues?.queryString), trimmedQueryString);

    try {
      return onSubmit({
        queryString,
        timerange: isNoTimeRangeOverride(timerange) ? undefined : normalizeFromSearchBarForBackend(timerange, userTimezone),
        ...rest,
      });
    } finally {
      setEnableReinitialize(true);
    }
  }, [initialValues?.queryString, onSubmit, userTimezone]);

  return { enableReinitialize, onSubmit: _onSubmit };
};

export default useSearchBarSubmit;
