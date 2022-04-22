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
import { isEmpty } from 'lodash';

import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import validateTimeRange from 'views/components/TimeRangeValidation';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import type { SearchBarControl } from 'views/types';
import { validatePluggableValues } from 'views/components/searchbar/pluggableSearchBarControlsHandler';

type FormValues = {
  queryString: string,
  timerange: TimeRange | NoTimeRangeOverride,
}

const validate = async <T extends FormValues>(
  values: T,
  limitDuration: number,
  setFieldWarning: (fieldName: string, warning: unknown) => void,
  validateQueryString: (values: T) => Promise<QueryValidationState>,
  pluggableSearchBarControls: Array<() => SearchBarControl>,
) => {
  const { timerange: nextTimeRange } = values;
  let errors = {};

  const timeRangeErrors = validateTimeRange(nextTimeRange, limitDuration);

  if (!isEmpty(timeRangeErrors)) {
    errors = { ...errors, timerange: timeRangeErrors };
  }

  const pluggableSearchBarControlsErrors = await validatePluggableValues(values, pluggableSearchBarControls);

  if (!isEmpty(pluggableSearchBarControlsErrors)) {
    errors = { ...errors, ...pluggableSearchBarControlsErrors };
  }

  const queryValidation = await validateQueryString(values);

  if (queryValidation?.status === 'OK') {
    setFieldWarning('queryString', undefined);

    return errors;
  }

  if (queryValidation?.status === 'WARNING') {
    setFieldWarning('queryString', queryValidation);

    return errors;
  }

  if (queryValidation?.status === 'ERROR') {
    setFieldWarning('queryString', undefined);

    return { ...errors, queryString: queryValidation };
  }

  return errors;
};

export default validate;
