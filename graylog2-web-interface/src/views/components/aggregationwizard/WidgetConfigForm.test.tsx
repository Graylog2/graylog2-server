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
import { render, screen } from 'wrappedTestingLibrary';

import WidgetConfigForm from 'views/components/aggregationwizard/WidgetConfigForm';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import ValidationStateProvider from '../contexts/ValidationStateProvider';
import ValidationStateContext from '../contexts/ValidationStateContext';

describe('WidgetConfigForm', () => {
  const WidgetConfigFormWithValidationState = ({ validate }: { validate: () => ({ [key: string]: string })}) => {
    return (
      <ValidationStateProvider>
        <ValidationStateContext.Consumer>
          {({ hasErrors }) => (
            <>
              <WidgetConfigForm initialValues={{}} onSubmit={() => {}} validate={validate} config={AggregationWidgetConfig.builder().build()}>
                <span>Hello world!</span>
              </WidgetConfigForm>
              <span>{hasErrors ? 'Form has errors' : 'Form has no errors'}</span>
            </>
          )}
        </ValidationStateContext.Consumer>
      </ValidationStateProvider>
    );
  };

  it('propagates validation state to context if it has errors', async () => {
    render(<WidgetConfigFormWithValidationState validate={() => ({ visualization: 'Is missing.' })} />);

    await screen.findByText('Form has errors');
  });

  it('propagates validation state to context if it has no errors', async () => {
    render(<WidgetConfigFormWithValidationState validate={() => ({})} />);

    await screen.findByText('Form has no errors');
  });
});
