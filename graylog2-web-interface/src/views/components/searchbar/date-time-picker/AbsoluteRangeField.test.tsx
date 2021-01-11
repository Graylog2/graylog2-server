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
import { render } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import AbsoluteRangeField from './AbsoluteRangeField';

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

const initialValues = {
  nextTimeRange: defaultProps.originalTimeRange,
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
});
