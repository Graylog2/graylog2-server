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
import { useCallback, useState } from 'react';

import type { SearchBarFormValues } from 'views/Constants';
import recordQueryStringUsage from 'views/logic/queries/recordQueryStringUsage';
import useUserDateTime from 'hooks/useUserDateTime';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import { normalizeFromSearchBarForBackend } from 'views/logic/queries/NormalizeTimeRange';
import type { TimeRange } from 'views/logic/queries/Query';

type FormValues = {
  queryString: string;
  timerange: TimeRange;
};

const _trim = (s: string | undefined) => (s === undefined ? undefined : s.trim());

const useSearchBarSubmit = (initialValues: FormValues, onSubmit: (v: FormValues) => Promise<unknown>) => {
  const { userTimezone } = useUserDateTime();
  const [enableReinitialize, setEnableReinitialize] = useState(true);
  const _onSubmit = useCallback(
    async (values: SearchBarFormValues) => {
      setEnableReinitialize(false);
      const { queryString, timerange, ...rest } = values;

      const trimmedQueryString = _trim(queryString);
      await recordQueryStringUsage(trimmedQueryString, _trim(initialValues?.queryString));

      try {
        return onSubmit({
          queryString,
          timerange: isNoTimeRangeOverride(timerange)
            ? undefined
            : normalizeFromSearchBarForBackend(timerange, userTimezone),
          ...rest,
        });
      } finally {
        setEnableReinitialize(true);
      }
    },
    [initialValues?.queryString, onSubmit, userTimezone],
  );

  return { enableReinitialize, onSubmit: _onSubmit };
};

export default useSearchBarSubmit;
