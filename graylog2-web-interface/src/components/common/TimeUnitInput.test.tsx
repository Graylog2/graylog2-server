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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import TimeUnitInput from './TimeUnitInput';

describe('<TimeUnitInput />', () => {
  const findCheckbox = () => screen.findByTitle('Toggle time');
  const queryCheckbox = () => screen.queryByTitle('Toggle time');
  const findTimeInput = () => screen.findByRole('spinbutton', { name: /time unit input/i });
  const findUnitDropdown = (unitName = 'seconds') => screen.findByRole('button', { name: new RegExp(unitName, 'i') });

  it('should have right default values from default props', async () => {
    const onUpdate = jest.fn();

    render(<TimeUnitInput update={onUpdate} />);

    const checkbox = await findCheckbox();

    expect(checkbox).not.toBeChecked();
    expect(await findTimeInput()).toHaveValue(1);

    await findUnitDropdown();

    userEvent.click(checkbox);

    await waitFor(() => expect(onUpdate).toHaveBeenCalledWith(1, 'SECONDS', true));
  });

  it('should use custom default values', async () => {
    const onUpdate = jest.fn();

    render(<TimeUnitInput update={onUpdate} defaultValue={42} defaultEnabled />);

    const checkbox = await findCheckbox();

    expect(checkbox).toBeChecked();
    expect(await findTimeInput()).toHaveValue(42);

    await findUnitDropdown();

    userEvent.click(checkbox);

    await waitFor(() => expect(onUpdate).toHaveBeenCalledWith(42, 'SECONDS', false));
  });

  it('should use custom unit values', async () => {
    const onUpdate = jest.fn();

    render(<TimeUnitInput update={onUpdate} unit="DAYS" defaultEnabled />);

    const checkbox = await findCheckbox();

    expect(checkbox).toBeChecked();
    expect(await findTimeInput()).toHaveValue(1);

    await findUnitDropdown('days');

    userEvent.click(checkbox);

    await waitFor(() => expect(onUpdate).toHaveBeenCalledWith(1, 'DAYS', false));
  });

  it('should use values before default values', async () => {
    const onUpdate = jest.fn();

    render(<TimeUnitInput update={onUpdate} value={124} defaultValue={42} enabled={false} defaultEnabled />);

    const checkbox = await findCheckbox();

    expect(checkbox).not.toBeChecked();
    expect(await findTimeInput()).toHaveValue(124);

    await findUnitDropdown();

    userEvent.click(checkbox);

    await waitFor(() => expect(onUpdate).toHaveBeenCalledWith(124, 'SECONDS', true));
  });

  it('should use required before enabled and default enabled', async () => {
    const onUpdate = jest.fn();

    render(<TimeUnitInput update={onUpdate} required enabled={false} defaultEnabled={false} />);

    expect(queryCheckbox()).not.toBeInTheDocument();

    const timeInput = await findTimeInput();

    userEvent.clear(timeInput);
    userEvent.paste(timeInput, '42');

    await waitFor(() => expect(onUpdate).toHaveBeenCalledWith(42, 'SECONDS', true));
  });

  it('should disable all inputs when disabled', async () => {
    render(<TimeUnitInput update={() => {}} enabled={false} />);

    expect(await findTimeInput()).toBeDisabled();
    expect(await findUnitDropdown()).toBeDisabled();
  });

  it('should not display checkbox when hideCheckbox is set', async () => {
    render(<TimeUnitInput update={() => {}} hideCheckbox />);

    expect(queryCheckbox()).not.toBeInTheDocument();
  });

  it('should use required and enabled when hideCheckbox is set', async () => {
    const { rerender } = render(<TimeUnitInput update={() => {}} required enabled={false} defaultEnabled={false} hideCheckbox />);

    expect(queryCheckbox()).not.toBeInTheDocument();
    expect(await findTimeInput()).toBeEnabled();
    expect(await findUnitDropdown()).toBeEnabled();

    rerender(<TimeUnitInput update={() => {}} enabled={false} defaultEnabled={false} hideCheckbox />);

    expect(queryCheckbox()).not.toBeInTheDocument();
    expect(await findTimeInput()).toBeDisabled();
    expect(await findUnitDropdown()).toBeDisabled();
  });

  it('should use default value when clearing the input', async () => {
    const onUpdate = jest.fn();

    render(<TimeUnitInput update={onUpdate} defaultEnabled value={9} defaultValue={42} />);

    userEvent.type(await findTimeInput(), '{backspace}');

    await waitFor(() => expect(onUpdate).toHaveBeenCalledWith(42, 'SECONDS', true));
  });

  it('should use default value when input receives some text', async () => {
    const onUpdate = jest.fn();
    render(<TimeUnitInput update={onUpdate} defaultEnabled value={9} defaultValue={42} />);
    const timeInput = await findTimeInput();
    userEvent.type(timeInput, '{backspace}adsasd');

    await waitFor(() => expect(onUpdate).toHaveBeenCalledWith(42, 'SECONDS', true));
  });

  describe('when clearable is set', () => {
    it('should use undefined when clearing input', async () => {
      const onUpdate = jest.fn();

      render(
        <TimeUnitInput update={onUpdate} defaultEnabled clearable value={9} defaultValue={42} />,
      );

      userEvent.type(await findTimeInput(), '{backspace}');
      await waitFor(() => expect(onUpdate).toHaveBeenCalledWith(undefined, 'SECONDS', true));
    });

    it('should use undefined when input receives some text', async () => {
      const onUpdate = jest.fn();

      render(
        <TimeUnitInput update={onUpdate} defaultEnabled clearable value={9} defaultValue={42} />,
      );

      userEvent.type(await findTimeInput(), '{backspace}adasd');
      await waitFor(() => expect(onUpdate).toHaveBeenCalledWith(undefined, 'SECONDS', true));
    });

    it('should render empty string when value is undefined', async () => {
      render(
        <TimeUnitInput update={() => {}} defaultEnabled clearable value={undefined} defaultValue={42} />,
      );

      expect(await findTimeInput()).toHaveValue(null);
    });
  });
});
