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
import PropTypes from 'prop-types';
import { Form, Formik } from 'formik';
import { isFunction } from 'lodash';
import type { FormikProps } from 'formik';

import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import FormWarningsContext from 'contexts/FormWarningsContext';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import validate from 'views/components/searchbar/validate';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import usePluginEntities from 'views/logic/usePluginEntities';

import { onInitializingTimerange, onSubmittingTimerange } from './TimerangeForForm';

export type DashboardFormValues = {
  timerange: TimeRange | undefined | null | NoTimeRangeOverride,
  queryString: string | undefined | null,
};

type Props = {
  initialValues: DashboardFormValues,
  limitDuration: number,
  onSubmit: (values: DashboardFormValues) => void | Promise<any>,
  children: ((props: FormikProps<DashboardFormValues>) => React.ReactElement) | React.ReactElement,
  validateQueryString: (values: DashboardFormValues) => Promise<QueryValidationState>,
};

const _isFunction = (children: Props['children']): children is (props: FormikProps<DashboardFormValues>) => React.ReactElement => isFunction(children);

const DashboardSearchForm = ({ initialValues, limitDuration, onSubmit, validateQueryString, children }: Props) => {
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar');
  const _onSubmit = useCallback(({ timerange, ...rest }: DashboardFormValues) => {
    return onSubmit({
      timerange: isNoTimeRangeOverride(timerange) ? undefined : onSubmittingTimerange(timerange),
      ...rest,
    });
  }, [onSubmit]);
  const { timerange, ...rest } = initialValues;
  const initialTimeRange = timerange && !isNoTimeRangeOverride(timerange) ? onInitializingTimerange(timerange) : {} as TimeRange;
  const _initialValues = {
    timerange: initialTimeRange,
    nextTimeRange: initialTimeRange,
    ...rest,
  };

  const { setFieldWarning } = useContext(FormWarningsContext);
  const _validate = useCallback((values: DashboardFormValues) => validate(values, limitDuration, setFieldWarning, validateQueryString, pluggableSearchBarControls),
    [limitDuration, setFieldWarning, validateQueryString, pluggableSearchBarControls]);

  return (
    <Formik<DashboardFormValues> initialValues={_initialValues}
                                 enableReinitialize
                                 onSubmit={_onSubmit}
                                 validate={_validate}
                                 validateOnMount>
      {(...args) => (
        <Form>
          {_isFunction(children) ? children(...args) : children}
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
  limitDuration: PropTypes.number.isRequired,
  onSubmit: PropTypes.func.isRequired,
};

export default DashboardSearchForm;
