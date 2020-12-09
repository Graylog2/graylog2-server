import * as React from 'react';
import { render } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import AbsoluteTimeRangeSelector from './AbsoluteTimeRangeSelector';

const defaultProps = {
  disabled: false,
  originalTimeRange: {
    type: 'absolute',
    from: '1955-05-11 06:15:00.000',
    to: '1985-25-10 08:18:00.000',
  },
  currentTimeRange: {
    type: 'absolute',
    from: '1955-05-11 06:15:00.000',
    to: '1985-25-10 08:18:00.000',
  },
} as const;

const renderWithForm = (element) => render((
  <Formik initialValues={{ nextTimeRange: defaultProps.originalTimeRange }}
          onSubmit={() => {}}>
    <Form>
      {element}
    </Form>
  </Formik>
));

describe('AbsoluteTimeRangeSelector', () => {
  it('renders', () => {
    const { asFragment } = renderWithForm((
      <AbsoluteTimeRangeSelector {...defaultProps} />
    ));

    expect(asFragment()).toMatchSnapshot();
  });
});
