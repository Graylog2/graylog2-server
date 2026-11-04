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
import { Formik, Form } from 'formik';
import { render, screen, act } from 'wrappedTestingLibrary';

import { changeLanguage } from 'i18n';

import TimezoneFormGroup from './TimezoneFormGroup';

describe('<TimezoneFormGroup />', () => {
  afterEach(() => changeLanguage('en'));

  it('follows language changes', async () => {
    render(
      <Formik initialValues={{ timezone: 'Europe/Berlin' }} onSubmit={jest.fn()}>
        <Form>
          <TimezoneFormGroup />
        </Form>
      </Formik>,
    );

    await screen.findByText('Time Zone');

    await act(() => changeLanguage('de'));

    await screen.findByText('Zeitzone');
    await screen.findByText(/Wählen Sie Ihre lokale Zeitzone/);
  });
});
