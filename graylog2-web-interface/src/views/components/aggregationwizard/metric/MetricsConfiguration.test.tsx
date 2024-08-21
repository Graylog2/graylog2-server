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
import React from 'react';
import * as Immutable from 'immutable';
import { Formik, Form } from 'formik';
import { render, screen } from 'wrappedTestingLibrary';

import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { asMock } from 'helpers/mocking';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import type { MetricFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType, { Properties } from 'views/logic/fieldtypes/FieldType';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import useFeature from 'hooks/useFeature';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import MetricsConfiguration from 'views/components/aggregationwizard/metric/MetricsConfiguration';

jest.mock('hooks/useFeature');
jest.mock('views/hooks/useAggregationFunctions');
jest.mock('views/hooks/useActiveQueryId');
jest.mock('views/logic/fieldtypes/useFieldTypes', () => jest.fn());

const NOT_ALLOWED_UNIT_FUNCTIONS = ['card', 'count', 'percentage'];
const ALLOWED_UNIT_FUNCTIONS = ['sum', 'latest', 'avg', 'min', 'max', 'percentage', 'stddev', 'variance', 'sumofsquares', 'percentile'];
const generateAllowedMetricsWithField = ():Array<MetricFormValues> => ALLOWED_UNIT_FUNCTIONS
  .map((fn, index) => ({ function: fn, field: `field-allowed-${index}`, name: `metric-name-allowed-${index}` }));
const generateAllowedMetricsWithoutField = ():Array<MetricFormValues> => ALLOWED_UNIT_FUNCTIONS
  .map((fn, index) => ({ function: fn, field: null, name: `metric-name-allowed-no-filed-${index}` }));

const generateNotAllowedMetricsWithField = ():Array<MetricFormValues> => NOT_ALLOWED_UNIT_FUNCTIONS
  .map((fn, index) => ({ function: fn, field: `field-not-allowed-${index}`, name: `metric-name-not-allowed-${index}` }));
const generateNotAllowedMetricsWithoutField = ():Array<MetricFormValues> => NOT_ALLOWED_UNIT_FUNCTIONS
  .map((fn, index) => ({ function: fn, field: null, name: `metric-name-not-allowed-no-filed-${index}` }));

const fieldTypes = [
  FieldTypeMapping.create('field-allowed-0',
    FieldType.create('number', [Properties.Numeric])),

  FieldTypeMapping.create('field-allowed-1',
    FieldType.create('number', [Properties.Numeric])),

  FieldTypeMapping.create('field-allowed-2',
    FieldType.create('number', [Properties.FullTextSearch])),

  FieldTypeMapping.create('field-allowed-3',
    FieldType.create('number', [Properties.FullTextSearch])),

  FieldTypeMapping.create('field-not-allowed-0',
    FieldType.create('number', [Properties.Numeric])),

  FieldTypeMapping.create('field-not-allowed-1',
    FieldType.create('number', [Properties.FullTextSearch])),
];

const SUT = ({ initialValues = {} }: { initialValues: WidgetConfigFormValues }) => (
  <TestStoreProvider>
    <FieldTypesContext.Provider value={{
      all: Immutable.List(fieldTypes),
      queryFields: Immutable.Map({
        queryId: Immutable.List(fieldTypes),
      }),
    }}>
      <Formik initialValues={initialValues} onSubmit={() => {}}>
        <Form>
          <MetricsConfiguration />
        </Form>
      </Formik>
    </FieldTypesContext.Provider>
  </TestStoreProvider>
);

describe('MetricsConfiguration', () => {
  useViewsPlugin();

  beforeEach(() => {
    asMock(useFeature).mockReturnValue(true);
    asMock(useActiveQueryId).mockReturnValue('queryId');

    asMock(useFieldTypes).mockImplementation(() => ({
      data: fieldTypes, isLoading: false, isFetching: false, refetch: () => {},
    }));
  });

  const metrics: Array<MetricFormValues> = [
    ...generateAllowedMetricsWithField(),
    ...generateAllowedMetricsWithoutField(),
    ...generateNotAllowedMetricsWithField(),
    ...generateNotAllowedMetricsWithoutField(),
    {
      function: 'count',
      name: null,
      field: null,
    },
  ];

  it('should render FieldUnit for allowed function and only numeric fields', async () => {
    render(<SUT initialValues={{ metrics: metrics }} />);

    const allMetricsWithField = await screen.findAllByTitle(/unit settings$/i);
    await screen.findByTitle(/field-allowed-0 unit settings/);
    await screen.findByTitle(/field-allowed-1 unit settings/);

    expect(allMetricsWithField.length).toEqual(2);
  });
});
