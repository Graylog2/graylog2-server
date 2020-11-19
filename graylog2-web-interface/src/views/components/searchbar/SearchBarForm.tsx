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
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { useCallback } from 'react';
import { Form, Formik } from 'formik';
import { isFunction } from 'lodash';
import type { FormikProps } from 'formik';

import DateTime from 'logic/datetimes/DateTime';
import type { TimeRange } from 'views/logic/queries/Query';
import { onInitializingTimerange, onSubmittingTimerange } from 'views/components/TimerangeForForm';

export type Values = {
  timerange: TimeRange,
  streams: Array<string>,
  queryString: string,
};

type Props = {
  initialValues: Values,
  onSubmit: (Values) => void | Promise<any>,
  children: ((props: FormikProps<Values>) => React.ReactNode) | React.ReactNode,
};

const validate = (values) => {
  return (values.timerange.type === 'absolute' && DateTime.isValidDateString(values.timerange.from) && values.timerange.from > values.timerange.to)
    ? {
      from: 'Start date must be before end date',
    }
    : {};
};

const StyledForm = styled(Form)`
  height: 100%;
`;

const _isFunction = (children: Props['children']): children is (props: FormikProps<Values>) => React.ReactElement => isFunction(children);

const SearchBarForm = ({ initialValues, onSubmit, children }: Props) => {
  const _onSubmit = useCallback(({ timerange, streams, queryString }) => {
    const newTimerange = onSubmittingTimerange(timerange);

    return onSubmit({
      timerange: newTimerange,
      streams,
      queryString,
    });
  }, [onSubmit]);
  const { timerange, streams, queryString } = initialValues;
  const _initialValues = {
    queryString,
    streams,
    timerange: onInitializingTimerange(timerange),
  };

  return (
    <Formik initialValues={_initialValues}
            enableReinitialize
            onSubmit={_onSubmit}
            validate={validate}>
      {(...args) => (
        <StyledForm>
          {_isFunction(children) ? children(...args) : children}
        </StyledForm>
      )}
    </Formik>
  );
};

SearchBarForm.propTypes = {
  initialValues: PropTypes.shape({
    timerange: PropTypes.object.isRequired,
    queryString: PropTypes.string.isRequired,
    streams: PropTypes.arrayOf(PropTypes.string).isRequired,
  }).isRequired,
  onSubmit: PropTypes.func.isRequired,
};

export default SearchBarForm;
