import { isEmpty } from 'lodash';

import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import validateTimeRange from 'views/components/TimeRangeValidation'; import { TimeRange } from 'views/logic/queries/Query';

type FormValues = {
  timerange: TimeRange,
}

const validate = async <T extends FormValues>(
  values: T,
  limitDuration: number,
  setFieldWarning: (fieldName: string, warning: unknown) => void,
  validateQueryString: (values: T) => Promise<QueryValidationState>,
) => {
  const { timerange: nextTimeRange } = values;
  let errors = {};

  const timeRangeErrors = validateTimeRange(nextTimeRange, limitDuration);

  if (!isEmpty(timeRangeErrors)) {
    errors = { ...errors, timerange: timeRangeErrors };
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
