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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import TabAbsoluteTimeRange from './TabAbsoluteTimeRange';

const defaultProps = {
  disabled: false,
  nextTimeRange: {
    type: 'absolute',
    from: '1955-05-11 06:15:00.000',
    to: '1985-10-25 08:18:00.000',
  },
} as const;

const renderWithForm = (element) => render((
  <Formik initialValues={{ nextTimeRange: defaultProps.nextTimeRange }}
          onSubmit={() => {}}>
    <Form>
      {element}
    </Form>
  </Formik>
));

describe('TabAbsoluteTimeRange', () => {
  it('renders Accordions that work', () => {
    renderWithForm((
      <TabAbsoluteTimeRange {...defaultProps} />
    ));

    const accordion = screen.getByTestId('absolute-time-ranges');
    const accordionItemCal = screen.getByRole('button', { name: 'Calendar' });
    const accordionItemTime = screen.getByRole('button', { name: 'Timestamp' });

    expect(accordion).not.toBeNull();
    expect(accordionItemCal.getAttribute('aria-expanded')).toEqual('true');
    expect(accordionItemTime.getAttribute('aria-expanded')).toEqual('false');

    fireEvent.click(accordionItemTime);

    expect(accordionItemCal.getAttribute('aria-expanded')).toEqual('false');
    expect(accordionItemTime.getAttribute('aria-expanded')).toEqual('true');
  });
});
