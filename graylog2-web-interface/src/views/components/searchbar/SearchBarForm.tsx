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
import PropTypes from 'prop-types';
import type { FormikProps } from 'formik';
import { Form, Formik } from 'formik';
import isFunction from 'lodash/isFunction';

import { SearchQueryStrings } from '@graylog/server-api';
import { onInitializingTimerange } from 'views/components/TimerangeForForm';
import { normalizeFromSearchBarForBackend } from 'views/logic/queries/NormalizeTimeRange';
import type { SearchBarFormValues } from 'views/Constants';
import FormWarningsContext from 'contexts/FormWarningsContext';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import validate from 'views/components/searchbar/validate';
import usePluginEntities from 'hooks/usePluginEntities';
import useUserDateTime from 'hooks/useUserDateTime';
import useHandlerContext from 'views/components/useHandlerContext';

type FormRenderer = (props: FormikProps<SearchBarFormValues>) => React.ReactNode;
type Props = {
  children: FormRenderer | React.ReactNode,
  initialValues: SearchBarFormValues,
  limitDuration: number,
  onSubmit: (values: SearchBarFormValues) => Promise<any>,
  validateOnMount?: boolean,
  formRef?: React.Ref<FormikProps<SearchBarFormValues>>,
  validateQueryString: (values: SearchBarFormValues) => Promise<QueryValidationState>,
}

const _isFunction = (children: Props['children']): children is FormRenderer => isFunction(children);

export const normalizeSearchBarFormValues = ({ timerange, ...rest }: SearchBarFormValues, userTimezone: string) => ({ timerange: normalizeFromSearchBarForBackend(timerange, userTimezone), ...rest });

const executeWithQueryStringRecording = async <R, >(isDirty: boolean, query: string, callback: () => R) => {
  const trimmedQuery = query.trim();

  try {
    if (isDirty && !!trimmedQuery) {
      await SearchQueryStrings.queryStringUsed({ query_string: trimmedQuery });
    }
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error('Unable to record last used query string: ', error);
  }

  return callback();
};

const SearchBarForm = ({ initialValues, limitDuration, onSubmit, children, validateOnMount, formRef, validateQueryString }: Props) => {
  const [enableReinitialize, setEnableReinitialize] = useState(true);
  const { formatTime, userTimezone } = useUserDateTime();
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar');
  const { setFieldWarning } = useContext(FormWarningsContext);
  const _initialValues = useMemo(() => {
    const { timerange, ...rest } = initialValues;

    return ({
      ...rest,
      timerange: onInitializingTimerange(timerange, formatTime),
    });
  }, [formatTime, initialValues]);

  const _onSubmit = useCallback((values: SearchBarFormValues) => {
    setEnableReinitialize(false);
    const queryString = values?.queryString;

    return executeWithQueryStringRecording(
      queryString !== _initialValues?.queryString,
      queryString,
      () => onSubmit(normalizeSearchBarFormValues(values, userTimezone)).finally(() => setEnableReinitialize(true)),
    );
  }, [_initialValues.queryString, onSubmit, userTimezone]);

  const handlerContext = useHandlerContext();
  const _validate = useCallback((values: SearchBarFormValues) => validate(values, limitDuration, setFieldWarning, validateQueryString, pluggableSearchBarControls, formatTime, handlerContext),
    [limitDuration, setFieldWarning, validateQueryString, pluggableSearchBarControls, formatTime, handlerContext]);

  return (
    <Formik<SearchBarFormValues> initialValues={_initialValues}
                                 enableReinitialize={enableReinitialize}
                                 onSubmit={_onSubmit}
                                 innerRef={formRef}
                                 validate={_validate}
                                 validateOnBlur={false}
                                 validateOnMount={validateOnMount}>
      {(...args) => (
        <Form>
          {_isFunction(children) ? children(...args) : children}
        </Form>
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
