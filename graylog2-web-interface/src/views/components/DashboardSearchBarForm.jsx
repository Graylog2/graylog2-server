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
// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import { Form, Formik } from 'formik';
import { isFunction } from 'lodash';
import type { FormikProps } from 'formik/@flow-typed';

import type { TimeRange } from 'views/logic/queries/Query';

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
