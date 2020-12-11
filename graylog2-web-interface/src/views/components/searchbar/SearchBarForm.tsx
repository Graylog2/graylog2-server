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
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { useCallback } from 'react';
import { Form, Formik } from 'formik';
import { isFunction } from 'lodash';
import type { FormikProps } from 'formik';

import DateTime from 'logic/datetimes/DateTime';
import { onInitializingTimerange, onSubmittingTimerange } from 'views/components/TimerangeForForm';
import type { SearchBarFormValues } from 'views/Constants';

type Props = {
  children: ((props: FormikProps<SearchBarFormValues>) => React.ReactNode) | React.ReactNode,
  initialValues: SearchBarFormValues,
  limitDuration: number,
  onSubmit: (Values) => void | Promise<any>,
};

export const dateTimeValidate = (values, limitDuration) => {
  const errors: { nextTimeRange?: {
    from?: string,
    to?: string,
    range?: string,
  } } = {};

  const { nextTimeRange } = values;

  if (nextTimeRange?.type === 'absolute') {
    if (!DateTime.isValidDateString(nextTimeRange.from)) {
      errors.nextTimeRange = { ...errors.nextTimeRange, from: 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]].' };
    }

    if (!DateTime.isValidDateString(nextTimeRange.to)) {
      errors.nextTimeRange = { ...errors.nextTimeRange, to: 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]].' };
    }

    if (nextTimeRange.from > nextTimeRange.to) {
      errors.nextTimeRange = { ...errors.nextTimeRange, from: 'Start date must be before end date' };
    }
  }

  if (nextTimeRange?.type === 'relative') {
    if (!(limitDuration === 0 || (nextTimeRange.range <= limitDuration && limitDuration !== 0))) {
      errors.nextTimeRange = { range: 'Range is outside limit duration.' };
    }
  }

  return errors;
};

const StyledForm = styled(Form)`
  height: 100%;
`;

const _isFunction = (children: Props['children']): children is (props: FormikProps<SearchBarFormValues>) => React.ReactElement => isFunction(children);

const SearchBarForm = ({ initialValues, onSubmit, children, limitDuration }: Props) => {
  const _onSubmit = useCallback(({ timerange, streams, queryString }) => {
    const newTimeRange = onSubmittingTimerange(timerange);

    return onSubmit({
      timerange: newTimeRange,
      streams,
      queryString,
    });
  }, [onSubmit]);
  const { timerange, streams, queryString } = initialValues;
  const initialTimeRange = onInitializingTimerange(timerange);
  const _initialValues = {
    limitDuration,
    queryString,
    streams,
    timerange: initialTimeRange,
    nextTimeRange: initialTimeRange,
  };

  return (
    <Formik initialValues={_initialValues}
            enableReinitialize
            onSubmit={_onSubmit}
            validate={(values) => dateTimeValidate(values, limitDuration)}>
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
    limitDuration: PropTypes.number.isRequired,
    timerange: PropTypes.object.isRequired,
    queryString: PropTypes.string.isRequired,
    streams: PropTypes.arrayOf(PropTypes.string).isRequired,
  }).isRequired,
  onSubmit: PropTypes.func.isRequired,
};

export default SearchBarForm;
