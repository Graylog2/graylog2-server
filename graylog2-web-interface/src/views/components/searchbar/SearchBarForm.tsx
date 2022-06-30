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
import { useCallback, useContext, useMemo, useState } from 'react';
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
import usePluginEntities from 'views/logic/usePluginEntities';
import UserDateTimeContext from 'contexts/UserDateTimeContext';

type Props = {
  children: ((props: FormikProps<SearchBarFormValues>) => React.ReactNode) | React.ReactNode,
  initialValues: SearchBarFormValues,
  limitDuration: number,
  onSubmit: (values: SearchBarFormValues) => Promise<any>,
  validateOnMount?: boolean,
  formRef?: React.Ref<FormikProps<SearchBarFormValues>>,
  validateQueryString: (values: SearchBarFormValues) => Promise<QueryValidationState>,
}

const StyledForm = styled(Form)`
  height: 100%;
`;

const _isFunction = (children: Props['children']): children is (props: FormikProps<SearchBarFormValues>) => React.ReactElement => isFunction(children);

export const normalizeSearchBarFormValues = ({ timerange, ...rest }: SearchBarFormValues, userTimezone: string) => ({ timerange: onSubmittingTimerange(timerange, userTimezone), ...rest });

const SearchBarForm = ({ initialValues, limitDuration, onSubmit, children, validateOnMount, formRef, validateQueryString }: Props) => {
  const [enableReinitialize, setEnableReinitialize] = useState(true);
  const { formatTime, userTimezone } = useContext(UserDateTimeContext);
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar');
  const { setFieldWarning } = useContext(FormWarningsContext);
  const _onSubmit = useCallback((values: SearchBarFormValues) => {
    setEnableReinitialize(false);

    return onSubmit(normalizeSearchBarFormValues(values, userTimezone)).finally(() => setEnableReinitialize(true));
  }, [onSubmit]);
  const _initialValues = useMemo(() => {
    const { timerange, ...rest } = initialValues;

    return ({
      ...rest,
      timerange: onInitializingTimerange(timerange, formatTime),
    });
  }, [formatTime, initialValues]);

  const _validate = useCallback((values: SearchBarFormValues) => validate(values, limitDuration, setFieldWarning, validateQueryString, pluggableSearchBarControls),
    [limitDuration, setFieldWarning, validateQueryString, pluggableSearchBarControls]);

  return (
    <Formik<SearchBarFormValues> initialValues={_initialValues}
                                 enableReinitialize={enableReinitialize}
                                 onSubmit={_onSubmit}
                                 innerRef={formRef}
                                 validate={_validate}
                                 validateOnBlur={false}
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
  onSubmit: PropTypes.func.isRequired,
  limitDuration: PropTypes.number.isRequired,
  validateOnMount: PropTypes.bool,
};

SearchBarForm.defaultProps = {
  validateOnMount: true,
  formRef: undefined,
};

export default SearchBarForm;
