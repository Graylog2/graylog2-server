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
import { Formik, Form } from 'formik';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import ObjectFields from './ObjectFields';

import type { TunableSchema, TunableValues } from '../types';
import validateTunable from '../validateTunable';

const schema: TunableSchema = {
  type: 'object',
  properties: {
    enabled: { type: 'boolean', title: 'Enabled', 'x-graylog-order': 1 },
    limit: {
      type: 'integer',
      title: 'Limit',
      minimum: 1,
      description: 'Max entries',
      'x-graylog-order': 2,
      'x-graylog-enabled-when': [{ property: 'enabled', in: ['true'] }],
    },
    mode: {
      type: 'string',
      title: 'Mode',
      enum: ['FAST', 'SAFE'],
      'x-graylog-order': 3,
    },
    grace: {
      type: 'string',
      format: 'duration',
      title: 'Grace period',
      'x-graylog-units': ['minutes', 'hours'],
      'x-graylog-order': 4,
    },
    note: { type: ['string', 'null'], title: 'Note', 'x-graylog-order': 5 },
    retries: { type: ['integer', 'null'], title: 'Retries', 'x-graylog-order': 9 },
  },
};

const initialValues: TunableValues = {
  enabled: false,
  limit: 5,
  mode: 'FAST',
  grace: 'PT90M',
  note: null,
  retries: null,
};

const renderForm = (onSubmit = jest.fn()) => {
  render(
    <Formik initialValues={initialValues} onSubmit={onSubmit} validate={(values) => validateTunable(schema, values)}>
      <Form>
        <ObjectFields schema={schema} />
        <button type="submit">Save</button>
      </Form>
    </Formik>,
  );

  return onSubmit;
};

const submit = async () => {
  await userEvent.click(screen.getByRole('button', { name: /save/i }));
};

describe('runtime tunable fields', () => {
  it('disables fields until their condition matches', async () => {
    renderForm();

    expect(await screen.findByRole('spinbutton', { name: /limit/i })).toBeDisabled();
    await userEvent.click(screen.getByRole('checkbox', { name: /enabled/i }));
    expect(screen.getByRole('spinbutton', { name: /limit/i })).toBeEnabled();
  });

  it('keeps the duration unit and submits ISO durations', async () => {
    const onSubmit = renderForm();

    await screen.findByDisplayValue('90');
    await submit();

    await screen.findByRole('button', { name: /save/i });
    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ grace: 'PT90M' }), expect.anything());
  });

  it('edits nullable numbers', async () => {
    const onSubmit = renderForm();

    await userEvent.type(await screen.findByRole('spinbutton', { name: /^retries/i }), '3');
    await submit();

    await screen.findByRole('button', { name: /save/i });
    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ retries: 3 }), expect.anything());
  });

  it('shows validation errors next to fields', async () => {
    renderForm();

    await userEvent.click(await screen.findByRole('checkbox', { name: /enabled/i }));
    const limit = screen.getByRole('spinbutton', { name: /limit/i });
    await userEvent.clear(limit);
    await userEvent.type(limit, '0');
    await submit();

    await screen.findByText('Must be at least 1');
  });
});
