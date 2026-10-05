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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import Radio from './Radio';

describe('Radio', () => {
  it('renders with label text', () => {
    render(
      <Radio checked={false} onChange={() => {}}>
        Option A
      </Radio>,
    );

    expect(screen.getByRole('radio')).toBeInTheDocument();
    expect(screen.getByText('Option A')).toBeInTheDocument();
  });

  it('is checked when checked prop is true', () => {
    render(
      <Radio checked onChange={() => {}}>
        Option A
      </Radio>,
    );

    expect(screen.getByRole('radio')).toBeChecked();
  });

  it('is unchecked when checked prop is false', () => {
    render(
      <Radio checked={false} onChange={() => {}}>
        Option A
      </Radio>,
    );

    expect(screen.getByRole('radio')).not.toBeChecked();
  });

  it('calls onChange when clicked', async () => {
    const onChange = jest.fn();
    render(
      <Radio checked={false} onChange={onChange}>
        Option A
      </Radio>,
    );

    await userEvent.click(screen.getByRole('radio'));

    expect(onChange).toHaveBeenCalledTimes(1);
  });

  it('is disabled when disabled prop is true', () => {
    render(
      <Radio checked={false} disabled onChange={() => {}}>
        Option A
      </Radio>,
    );

    expect(screen.getByRole('radio')).toBeDisabled();
  });

  it('forwards id to the input', () => {
    render(
      <Radio checked={false} id="my-radio" onChange={() => {}}>
        Option A
      </Radio>,
    );

    expect(screen.getByRole('radio')).toHaveAttribute('id', 'my-radio');
  });

  it('forwards name to the input', () => {
    render(
      <Radio checked={false} name="group" onChange={() => {}}>
        Option A
      </Radio>,
    );

    expect(screen.getByRole('radio')).toHaveAttribute('name', 'group');
  });

  it('forwards value to the input', () => {
    render(
      <Radio checked={false} value="option-a" onChange={() => {}}>
        Option A
      </Radio>,
    );

    expect(screen.getByRole('radio')).toHaveAttribute('value', 'option-a');
  });

  it('associates label with input via id', () => {
    render(
      <Radio checked={false} id="my-radio" onChange={() => {}}>
        Option A
      </Radio>,
    );

    expect(screen.getByLabelText('Option A')).toBeInTheDocument();
  });

  it('forwards aria-label to the input', () => {
    render(<Radio checked={false} aria-label="Select option A" onChange={() => {}} />);

    expect(screen.getByRole('radio', { name: 'Select option A' })).toBeInTheDocument();
  });

  it('forwards aria-describedby to the input', () => {
    render(
      <Radio checked={false} aria-describedby="hint-text" onChange={() => {}}>
        Option A
      </Radio>,
    );

    expect(screen.getByRole('radio')).toHaveAttribute('aria-describedby', 'hint-text');
  });

  it('forwards aria-labelledby to the input', () => {
    render(
      <Radio checked={false} aria-labelledby="label-id" onChange={() => {}}>
        Option A
      </Radio>,
    );

    expect(screen.getByRole('radio')).toHaveAttribute('aria-labelledby', 'label-id');
  });
});
