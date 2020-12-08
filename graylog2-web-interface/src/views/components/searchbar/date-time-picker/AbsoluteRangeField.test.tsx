import * as React from 'react';
import { act, fireEvent, render, screen } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import AbsoluteRangeField from './AbsoluteRangeField';

const defaultProps = {
  disabled: false,
  originalTimeRange: {
    type: 'absolute',
    from: '1955-05-11 06:15:00.000',
    to: '1985-25-10 08:18:00.000',
  },
  currentTimerange: {
    type: 'absolute',
    from: '1955-05-11 06:15:00.000',
    to: '1985-25-10 08:18:00.000',
  },
} as const;

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

describe('AbsoluteRangeField', () => {
  it('renders', () => {
    const { asFragment } = renderWithForm((
      <AbsoluteRangeField {...defaultProps} from />
    ));

    expect(asFragment()).toMatchSnapshot();
  });

  it('toggles bod & eod', () => {
    renderWithForm((
      <AbsoluteRangeField {...defaultProps} from />
    ));

    const toggleBtn = screen.getByRole('button', { name: /toggle between beginning and end of day/i });
    fireEvent.click(toggleBtn);

    const inputHour = screen.getByRole('textbox', { name: /from hour/i });
    const inputMinutes = screen.getByRole('textbox', { name: /from minutes/i });
    const inputSeconds = screen.getByRole('textbox', { name: /from seconds/i });
    const inputMillsecs = screen.getByRole('textbox', { name: /from milliseconds/i });

    expect(inputHour).toHaveValue('00');
    expect(inputMinutes).toHaveValue('00');
    expect(inputSeconds).toHaveValue('00');
    expect(inputMillsecs).toHaveValue('000');

    fireEvent.click(toggleBtn);

    expect(inputHour).toHaveValue('23');
    expect(inputMinutes).toHaveValue('59');
    expect(inputSeconds).toHaveValue('59');
    expect(inputMillsecs).toHaveValue('999');
  });

  it('does not allow non-numeric characters', () => {
    renderWithForm((
      <AbsoluteRangeField {...defaultProps} from />
    ));

    const inputHour = screen.getByRole('textbox', { name: /from hour/i });

    act(() => {
      fireEvent.change(inputHour, { target: { value: '/w!' } });
    });

    expect(inputHour).toHaveValue('00');
  });

  it('does allow proper value', () => {
    renderWithForm((
      <AbsoluteRangeField {...defaultProps} from />
    ));

    const inputHour = screen.getByRole('textbox', { name: /from hour/i });

    act(() => {
      fireEvent.change(inputHour, { target: { value: '10' } });
    });

    expect(inputHour).toHaveValue('10');
  });

  it('does not allow numbers over their maximum', () => {
    renderWithForm((
      <AbsoluteRangeField {...defaultProps} from />
    ));

    const inputHour = screen.getByRole('textbox', { name: /from hour/i });

    act(() => {
      fireEvent.change(inputHour, { target: { value: '50' } });
    });

    expect(inputHour).toHaveValue('23');
  });

  it('does not try to parse an empty date', () => {
    renderWithForm((
      <AbsoluteRangeField {...defaultProps} from />
    ));

    const inputHour = screen.getByRole('textbox', { name: /from hour/i });

    act(() => {
      fireEvent.change(inputHour, { target: { value: '' } });
    });

    expect(inputHour).toHaveValue('00');
  });
});
