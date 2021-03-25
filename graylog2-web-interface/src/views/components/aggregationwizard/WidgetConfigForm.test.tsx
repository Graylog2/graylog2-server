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
import { useState } from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import WidgetConfigForm from 'views/components/aggregationwizard/WidgetConfigForm';

import { ValidationStateContext } from '../widgets/EditWidgetFrame';

describe('WidgetConfigForm', () => {
  const WidgetConfigFormWithValidationState = ({ validate }: { validate: () => ({ [key: string]: string })}) => {
    const [hasErrors, setHasErrors] = useState(false);

    return (
      <ValidationStateContext.Provider value={[hasErrors, setHasErrors]}>
        <WidgetConfigForm initialValues={{}} onSubmit={() => {}} validate={validate}>
          <span>Hello world!</span>
        </WidgetConfigForm>
        <span>{hasErrors ? 'Form has errors' : 'Form has no errors'}</span>
      </ValidationStateContext.Provider>
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
