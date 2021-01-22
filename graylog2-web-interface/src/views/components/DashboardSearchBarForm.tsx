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
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import { Form, Formik } from 'formik';
import { isFunction } from 'lodash';
import type { FormikProps } from 'formik';

import type { TimeRange } from 'views/logic/queries/Query';

import DateTimeProvider from './searchbar/date-time-picker/DateTimeProvider';
import { onInitializingTimerange, onSubmittingTimerange } from './TimerangeForForm';
import { dateTimeValidate } from './searchbar/date-time-picker/TimeRangeDropdown';

type Values = {
  timerange: TimeRange | undefined | null,
  queryString: string | undefined | null,
};

type Props = {
  initialValues: Values,
  limitDuration: number,
  onSubmit: (Values) => void | Promise<any>,
  children: ((props: FormikProps<Values>) => React.ReactElement) | React.ReactElement,
};

const _isFunction = (children: Props['children']): children is (props: FormikProps<Values>) => React.ReactElement => isFunction(children);

const DashboardSearchForm = ({ initialValues, limitDuration, onSubmit, children }: Props) => {
  const _onSubmit = useCallback(({ timerange, queryString }) => {
    return onSubmit({
      timerange: Object.keys(timerange).length ? onSubmittingTimerange(timerange) : undefined,
      queryString,
    });
  }, [onSubmit]);
  const { timerange, queryString } = initialValues;
  const initialTimeRange = timerange ? onInitializingTimerange(timerange) : {};
  const _initialValues = {
    timerange: initialTimeRange,
    nextTimeRange: initialTimeRange,
    queryString,
  };

  return (
    <Formik initialValues={_initialValues}
            enableReinitialize
            onSubmit={_onSubmit}
            validate={({ timerange: nextTimeRange }) => dateTimeValidate(nextTimeRange, limitDuration)}
            validateOnMount>
      {(...args) => (
        <DateTimeProvider limitDuration={limitDuration}>
          <Form>
            {_isFunction(children) ? children(...args) : children}
          </Form>
        </DateTimeProvider>
      )}
    </Formik>
  );
};

DashboardSearchForm.propTypes = {
  initialValues: PropTypes.shape({
    timerange: PropTypes.object,
    queryString: PropTypes.string,
  }).isRequired,
  limitDuration: PropTypes.number.isRequired,
  onSubmit: PropTypes.func.isRequired,
};

export default DashboardSearchForm;
