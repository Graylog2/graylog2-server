// @flow strict
import * as React from 'react';
import { asElement, fireEvent, render, waitFor } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';
import { act } from 'react-dom/test-utils';

import AbsoluteTimeRangeSelector from './AbsoluteTimeRangeSelector';

const renderWithForm = (element) => render((
  <Formik initialValues={{ tempTimeRange: { type: 'absolute', from: '2020-01-16 10:04:30.329', to: '2020-01-16 12:04:30.329' } }}
          onSubmit={() => {}}>
    <Form>
      {element}
    </Form>
  </Formik>
));

const _findValidationState = (container) => {
  const formGroup = container?.matches('.form-group') ? container : container?.querySelector('.form-group');

  return formGroup && formGroup.className.includes('has-error')
    ? 'error'
    : null;
};

const _findFormGroup = (element) => element.closest('.form-group');

const getValidationStateOfInput = (input) => _findValidationState(_findFormGroup(input));

const changeInput = async (input, value) => {
  const { name } = asElement(input, HTMLInputElement);

  await act(async () => { fireEvent.change(input, { target: { value, name } }); });
};

const defaultProps = {
  disabled: false,
  originalTimeRange: {
    type: 'absolute',
    from: '1955-5-11 06:15:00.000',
    to: '1985-25-10 08:18:00.000',
  },
};

describe('AbsoluteTimeRangeSelector', () => {
  it('does not try to parse an empty date in from field', async () => {
    const { getByDisplayValue } = renderWithForm((
      <AbsoluteTimeRangeSelector {...defaultProps} />
    ));
    const fromDate = getByDisplayValue('2020-01-16 10:04:30.329');

    await changeInput(fromDate, '');

    await waitFor(() => expect(getValidationStateOfInput(fromDate)).toEqual('error'));
  });

  it('does not try to parse an empty date in to field', async () => {
    const { getByDisplayValue } = renderWithForm((
      <AbsoluteTimeRangeSelector {...defaultProps} />
    ));
    const toDate = getByDisplayValue('2020-01-16 12:04:30.329');

    await changeInput(toDate, '');

    await waitFor(() => expect(getValidationStateOfInput(toDate)).toEqual('error'));
  });

  it('shows error message for from date if parsing fails after changing input', async () => {
    const { getByDisplayValue, queryByText } = renderWithForm((
      <AbsoluteTimeRangeSelector {...defaultProps} />
    ));

    const fromDate = getByDisplayValue('2020-01-16 10:04:30.329');

    await changeInput(fromDate, 'invalid');

    await waitFor(() => expect(queryByText('Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]]')).not.toBeNull());
  });

  it('shows error message for to date if parsing fails after changing input', async () => {
    const { getByDisplayValue, queryByText } = renderWithForm((
      <AbsoluteTimeRangeSelector {...defaultProps} />
    ));

    const fromDate = getByDisplayValue('2020-01-16 12:04:30.329');

    await changeInput(fromDate, 'invalid');

    await waitFor(() => expect(queryByText('Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]]')).not.toBeNull());
  });
});
