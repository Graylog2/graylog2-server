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
import { useCallback, useContext } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import type { FormikProps } from 'formik';
import { Form, Formik } from 'formik';
import { isFunction } from 'lodash';

import { onInitializingTimerange, onSubmittingTimerange } from 'views/components/TimerangeForForm';
import type { SearchBarFormValues } from 'views/Constants';
import FormWarningsContext from 'contexts/FormWarningsContext';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import validate from 'views/components/searchbar/validate';

type Props = {
  children: ((props: FormikProps<SearchBarFormValues>) => React.ReactNode) | React.ReactNode,
  initialValues: SearchBarFormValues,
  limitDuration: number,
  onSubmit: (values: SearchBarFormValues) => void | Promise<any>,
  validateOnMount?: boolean,
  formRef?: React.Ref<FormikProps<SearchBarFormValues>>,
  validateQueryString: (values: SearchBarFormValues) => Promise<QueryValidationState>,
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

const SearchBarForm = ({ initialValues, limitDuration, onSubmit, children, validateOnMount, formRef, validateQueryString }: Props) => {
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

  const { setFieldWarning } = useContext(FormWarningsContext);
  const _validate = useCallback((values: SearchBarFormValues) => validate(values, limitDuration, setFieldWarning, validateQueryString),
    [limitDuration, setFieldWarning, validateQueryString]);

  return (
    <Formik<SearchBarFormValues> initialValues={_initialValues}
                                 enableReinitialize
                                 onSubmit={_onSubmit}
                                 innerRef={formRef}
                                 validate={_validate}
                                 validateOnMount={validateOnMount}>
      {(...args) => (
        <StyledForm>
          {_isFunction(children) ? children(...args) : children}
        </StyledForm>
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
