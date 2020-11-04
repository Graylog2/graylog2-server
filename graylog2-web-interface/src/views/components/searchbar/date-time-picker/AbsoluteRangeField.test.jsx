// @flow strict
import * as React from 'react';
// import { asElement, fireEvent, render, waitFor } from 'wrappedTestingLibrary';
import { fireEvent, render, screen } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';
// import { act } from 'react-dom/test-utils';

import AbsoluteRangeField from './AbsoluteRangeField';

const defaultProps = {
  disabled: false,
  originalTimeRange: {
    from: '1955-05-11 06:15:00.000',
    to: '1985-25-10 08:18:00.000',
  },
};

const initialValues = {
  tempTimeRange: defaultProps.originalTimeRange,
};

const renderWithForm = (element) => render((
  <Formik initialValues={initialValues}
          onSubmit={() => {}}>
    <Form>
      {element}
    </Form>
  </Formik>
));

// const _findValidationState = (container) => {
//   const formGroup = container?.matches('.form-group') ? container : container?.querySelector('.form-group');
//
//   return formGroup && formGroup.className.includes('has-error')
//     ? 'error'
//     : null;
// };

// const _findFormGroup = (element) => element.closest('.form-group');

// const getValidationStateOfInput = (input) => _findValidationState(_findFormGroup(input));

// const changeInput = async (input, value) => {
//   const { name } = asElement(input, HTMLInputElement);
//
//   await act(async () => { fireEvent.change(input, { target: { value, name } }); });
// };

describe('AbsoluteRangeField', () => {
  it('renders', () => {
    const { asFragment } = renderWithForm((
      <AbsoluteRangeField {...defaultProps} />
    ));

    expect(asFragment()).toMatchSnapshot();
  });

  it('toggles bod & eod', () => {
    renderWithForm((
      <AbsoluteRangeField {...defaultProps} from />
    ));

    const toggleBtn = screen.getByRole('button', { name: /toggle between beginning and end of day/i });
    fireEvent.click(toggleBtn);

    const bodHoursMinsSecs = screen.queryAllByDisplayValue('00');
    const bodMillisecs = screen.queryAllByDisplayValue('000');

    expect(bodHoursMinsSecs.length).toBe(3);
    expect(bodMillisecs.length).toBe(1);

    fireEvent.click(toggleBtn);

    const eodHours = screen.queryAllByDisplayValue('23');
    const eodMinsSecs = screen.queryAllByDisplayValue('59');
    const eodMillisecs = screen.queryAllByDisplayValue('999');

    expect(eodHours.length).toBe(1);
    expect(eodMinsSecs.length).toBe(2);
    expect(eodMillisecs.length).toBe(1);
  });

  // it('allows manually setting time', async () => {
  // });

  // it('does not try to parse an empty date', async () => {
  //   const { getByDisplayValue } = renderWithForm((
  //     <AbsoluteRangeField {...defaultProps} />
  //   ));
  //   const fromDate = getByDisplayValue('2020-01-16 10:04:30.329');
  //
  //   await changeInput(fromDate, '');
  //
  //   await waitFor(() => expect(getValidationStateOfInput(fromDate)).toEqual('error'));
  // });

  // it('shows error message for date if parsing fails after changing input', async () => {
  //   const { getByDisplayValue, queryByText } = renderWithForm((
  //     <AbsoluteRangeField {...defaultProps} />
  //   ));
  //
  //   const fromDate = getByDisplayValue('2020-01-16 10:04:30.329');
  //
  //   await changeInput(fromDate, 'invalid');
  //
  //   await waitFor(() => expect(queryByText('Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]]')).not.toBeNull());
  // });
});
