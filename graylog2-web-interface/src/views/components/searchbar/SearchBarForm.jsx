// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { useCallback } from 'react';
import { Form, Formik } from 'formik';
import { isFunction } from 'lodash';
import type { FormikProps } from 'formik/@flow-typed';

import DateTime from 'logic/datetimes/DateTime';
import type { TimeRange } from 'views/logic/queries/Query';
import { onInitializingTimerange, onSubmittingTimerange } from 'views/components/TimerangeForForm';

type Values = {
  tempTimeRange: TimeRange,
  timerange: TimeRange,
  streams: Array<string>,
  queryString: string,
  limitDuration: number,
};

type Props = {
  initialValues: Values,
  onSubmit: (Values) => void | Promise<any>,
  children: ((props: FormikProps<Values>) => React$Node) | React$Node,
};

const StyledForm = styled(Form)`
  height: 100%;
`;

export const validateTimeRanges = (values) => {
  const errors = { };

  if (values.tempTimeRange?.type === 'relative'
    && !(values.limitDuration === 0 || (values.tempTimeRange?.range <= values.limitDuration && values.limitDuration !== 0))) {
    errors.tempTimeRange = {
      range: 'Range is outside limit duration.',
    };
  }

  if (values.tempTimeRange?.type === 'absolute'
    && DateTime.isValidDateString(values.tempTimeRange.from)
    && values.tempTimeRange.from > values.tempTimeRange.to) {
    errors.tempTimeRange = {
      from: 'Start date must be before end date',
    };
  }

  return errors;
};

const SearchBarForm = ({ initialValues, onSubmit, children }: Props) => {
  const _onSubmit = useCallback(({ timerange, streams, queryString }) => {
    const newTimerange = onSubmittingTimerange(timerange);

    return onSubmit({
      timerange: newTimerange,
      streams,
      queryString,
    });
  }, [onSubmit]);
  const { limitDuration, timerange, streams, queryString } = initialValues;
  const initialTimeRange = onInitializingTimerange(timerange);
  const _initialValues: Values = {
    queryString,
    streams,
    timerange: initialTimeRange,
    tempTimeRange: initialTimeRange,
    limitDuration,
  };

  return (
    <Formik initialValues={_initialValues}
            enableReinitialize
            onSubmit={_onSubmit}
            validate={validateTimeRanges}>
      {(...args) => (
        <StyledForm>
          {isFunction(children) ? children(...args) : children}
        </StyledForm>
      )}
    </Formik>
  );
};

SearchBarForm.propTypes = {
  initialValues: PropTypes.shape({
    timerange: PropTypes.object.isRequired,
    queryString: PropTypes.string.isRequired,
    limitDuration: PropTypes.number.isRequired,
    streams: PropTypes.arrayOf(PropTypes.string).isRequired,
  }).isRequired,
  onSubmit: PropTypes.func.isRequired,
};

export default SearchBarForm;
