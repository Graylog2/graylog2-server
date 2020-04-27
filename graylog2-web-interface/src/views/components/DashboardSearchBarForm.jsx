// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import { Form, Formik } from 'formik';
import { isFunction } from 'lodash';

import type { TimeRange } from 'views/logic/queries/Query';
import type { FormikProps } from 'formik/@flow-typed';
import { onInitializingTimerange, onSubmittingTimerange } from './TimerangeForForm';

type Values = {
  timerange: ?TimeRange,
  queryString: ?string,
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
  const { timerange, queryString } = initialValues;
  const _initialValues = {
    timerange: timerange ? onInitializingTimerange(timerange) : timerange,
    queryString,
  };
  return (
    <Formik initialValues={_initialValues}
            enableReinitialize
            onSubmit={_onSubmit}>
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
  }).isRequired,
  onSubmit: PropTypes.func.isRequired,
};

export default DashboardSearchForm;
