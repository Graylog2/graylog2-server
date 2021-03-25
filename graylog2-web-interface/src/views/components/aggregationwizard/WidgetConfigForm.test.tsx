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
