// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import { Form, Formik } from 'formik';
import { isFunction } from 'lodash';
import type { FormikProps } from 'formik/@flow-typed';

import type { TimeRange } from 'views/logic/queries/Query';
import { validateTimeRanges } from 'views/components/searchbar/SearchBarForm';

import { onInitializingTimerange, onSubmittingTimerange } from './TimerangeForForm';

type Values = {
  timerange: ?TimeRange,
  queryString: ?string,
  limitDuration: number,
};

type Props = {
  initialValues: Values,
  onSubmit: (Values) => void | Promise<any>,
  children: ((props: FormikProps<Values>) => React$Node) | React$Node,
};

const DashboardSearchForm = ({ initialValues, onSubmit, children }: Props) => {
  const _onSubmit = useCallback(({ timerange, queryString }) => {
    return onSubmit({
      timerange: timerange ? onSubmittingTimerange(timerange) : undefined,
      queryString,
    });
  }, [onSubmit]);
  const { timerange, limitDuration, queryString } = initialValues;
  const initialTimeRange = timerange ? onInitializingTimerange(timerange) : timerange;
  const _initialValues = {
    timerange: initialTimeRange,
    tempTimeRange: initialTimeRange,
    limitDuration,
    queryString,
  };

  return (
    <Formik initialValues={_initialValues}
            enableReinitialize
            onSubmit={_onSubmit}
            validate={validateTimeRanges}>
      {(...args) => (
        <Form>
          {isFunction(children) ? children(...args) : children}
        </Form>
      )}
    </Formik>
  );
};

DashboardSearchForm.propTypes = {
  initialValues: PropTypes.shape({
    timerange: PropTypes.object,
    queryString: PropTypes.string,
    limitDuration: PropTypes.number,
  }).isRequired,
  onSubmit: PropTypes.func.isRequired,
};

export default DashboardSearchForm;
