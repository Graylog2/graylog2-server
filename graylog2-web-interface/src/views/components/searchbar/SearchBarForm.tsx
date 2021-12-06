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
import styled from 'styled-components';
import PropTypes from 'prop-types';
import type { FormikProps } from 'formik';
import { Form, Formik } from 'formik';
import { isFunction, isEmpty } from 'lodash';

import FormWarningsProvider from 'contexts/FormWarningsProvider';
import { onInitializingTimerange, onSubmittingTimerange } from 'views/components/TimerangeForForm';
import type { SearchBarFormValues } from 'views/Constants';
import validateTimeRange from 'views/components/TimeRangeValidation';

import DateTimeProvider from './date-time-picker/DateTimeProvider';

type Props = {
  children: ((props: FormikProps<SearchBarFormValues>) => React.ReactNode) | React.ReactNode,
  initialValues: SearchBarFormValues,
  limitDuration: number,
  onSubmit: (values: SearchBarFormValues) => void | Promise<any>,
  validateOnMount?: boolean,
  formRef?: React.Ref<FormikProps<SearchBarFormValues>>,
}

const StyledForm = styled(Form)`
  height: 100%;
`;

const _isFunction = (children: Props['children']): children is (props: FormikProps<SearchBarFormValues>) => React.ReactElement => isFunction(children);

export const normalizeSearchBarFormValues = ({ timerange, streams, queryString }) => {
  const newTimeRange = onSubmittingTimerange(timerange);

  return {
    timerange: newTimeRange,
    streams,
    queryString,
  };
};

const validate = (nextTimeRange, limitDuration) => {
  let errors = {};

  const timeRangeErrors = validateTimeRange(nextTimeRange, limitDuration);

  if (!isEmpty(timeRangeErrors)) {
    errors = { ...errors, timerange: timeRangeErrors };
  }

  return errors;
};

const SearchBarForm = ({ initialValues, limitDuration, onSubmit, children, validateOnMount, formRef }: Props) => {
  const _onSubmit = useCallback(({ timerange, streams, queryString }: SearchBarFormValues) => {
    return onSubmit(normalizeSearchBarFormValues({ timerange, streams, queryString }));
  }, [onSubmit]);
  const { timerange, streams, queryString } = initialValues;
  const initialTimeRange = onInitializingTimerange(timerange);
  const _initialValues = {
    queryString,
    streams,
    timerange: initialTimeRange,
  };

  return (
    <Formik<SearchBarFormValues> initialValues={_initialValues}
                                 enableReinitialize
                                 onSubmit={_onSubmit}
                                 innerRef={formRef}
                                 validate={({ timerange: nextTimeRange }) => validate(nextTimeRange, limitDuration)}
                                 validateOnMount={validateOnMount}>
      {(...args) => (
        <FormWarningsProvider>
          <DateTimeProvider limitDuration={limitDuration}>
            <StyledForm>
              {_isFunction(children) ? children(...args) : children}
            </StyledForm>
          </DateTimeProvider>
        </FormWarningsProvider>
      )}
    </Formik>
  );
};

SearchBarForm.propTypes = {
  initialValues: PropTypes.exact({
    timerange: PropTypes.object.isRequired,
    queryString: PropTypes.string.isRequired,
    streams: PropTypes.arrayOf(PropTypes.string).isRequired,
  }).isRequired,
  onSubmit: PropTypes.func.isRequired,
  limitDuration: PropTypes.number.isRequired,
  validateOnMount: PropTypes.bool,
};

SearchBarForm.defaultProps = {
  validateOnMount: true,
  formRef: undefined,
};

export default SearchBarForm;
