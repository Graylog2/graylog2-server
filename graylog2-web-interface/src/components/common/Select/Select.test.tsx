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
import userEvent from '@testing-library/user-event';
import React from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import selectEvent from 'helpers/selectEvent';

import Select from './index';

const createOption = (x) => ({ label: x, value: x });

describe('Select', () => {
  const defaultOptions = [
    { label: 'label1', value: 'value1' },
    { label: 'label2', value: 'value2' },
  ];
  const SimpleSelect = ({
    options = defaultOptions,
    onChange = () => {},
    ...rest
  }: Partial<React.ComponentProps<typeof Select>>) => (
    <Select className="simple-select" placeholder="Select value" options={options} onChange={onChange} {...rest} />
  );

  it('calls `onChange` upon selecting option', async () => {
    const options = ['foo', 'bar'].map(createOption);

    const onChange = jest.fn();

    render(<SimpleSelect options={options} onChange={onChange} />);

    const select = await screen.findByLabelText('Select value');

    await selectEvent.openMenu(select);

    await selectEvent.select(select, 'foo');

    await waitFor(() => expect(onChange).toHaveBeenCalledWith('foo'));
  });

  it('works with non-string options', async () => {
    const options = [23, 42].map(createOption);

    const onChange = jest.fn();

    render(<SimpleSelect options={options} onChange={onChange} />);

    const select = await screen.findByLabelText('Select value');

    await selectEvent.openMenu(select);

    await selectEvent.select(select, '42');

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(42));
  });

  describe('Upgrade to react-select v2', () => {
    it('should support multiple values', async () => {
      const onChange = jest.fn();
      render(<SimpleSelect onChange={onChange} multi />);

      const select = await screen.findByLabelText('Select value');

      selectEvent.openMenu(select);
      selectEvent.select(select, ['label1', 'label2']);

      await waitFor(() => expect(onChange).toHaveBeenCalledWith('value1,value2'));
    });

    it('should disable select', async () => {
      render(<SimpleSelect disabled />);

      expect(await screen.findByLabelText('Select value')).toBeDisabled();
    });

    it('should clear to isClearable', async () => {
      const onChange = jest.fn();
      const { container } = render(<SimpleSelect onChange={onChange} clearable />);

      const select = await screen.findByLabelText('Select value');

      selectEvent.openMenu(select);
      selectEvent.select(select, 'label1');

      await waitFor(() => expect(onChange).toHaveBeenCalledWith('value1'));

      selectEvent.clearAll(container, 'simple-select');

      expect(onChange).toHaveBeenCalledWith('');
    });

    it('should use displayKey to select the option label', async () => {
      const onChange = jest.fn();
      const customOptions = [{ customLabel: 'my great label', value: 'value1' }];
      render(<SimpleSelect options={customOptions} onChange={onChange} displayKey="customLabel" menuIsOpen />);

      const select = await screen.findByLabelText('Select value');
      selectEvent.openMenu(select);

      await screen.findByRole('option', { name: 'my great label' });
    });

    it('should use valueKey to select the option value', async () => {
      const onChange = jest.fn();
      const customOptions = [{ label: 'label1', customValue: 42 }];
      render(<SimpleSelect options={customOptions} onChange={onChange} valueKey="customValue" menuIsOpen />);

      const select = await screen.findByLabelText('Select value');
      selectEvent.openMenu(select);

      selectEvent.select(select, 'label1');

      await waitFor(() => expect(onChange).toHaveBeenCalledWith(42));
    });

    it('should use matchProp to configure how options are filtered', async () => {
      render(<SimpleSelect matchProp="value" />);

      const select = await screen.findByLabelText('Select value');
      selectEvent.openMenu(select);
      userEvent.type(select, 'value1');

      await screen.findByRole('option', { name: /label1/ });

      expect(screen.queryByRole('option', { name: /label2/ })).not.toBeInTheDocument();
    });

    it("should use optionRenderer to customize options' appearance", async () => {
      const optionRenderer = (option: { label: string }) => <span>Custom {option.label}</span>;
      render(<SimpleSelect optionRenderer={optionRenderer} menuIsOpen />);
      const select = await screen.findByLabelText('Select value');

      selectEvent.openMenu(select);
      await screen.findByRole('option', { name: /Custom label1/i });
    });

    it("should use valueRenderer to customize selected value's appearance", async () => {
      const valueRenderer = (option: { value: string }) => <span>Custom {option.value}</span>;
      render(<SimpleSelect valueRenderer={valueRenderer} value={defaultOptions[0].value} />);
      await screen.findByText('Custom value1');
    });

    it('should disable options that include a disabled property', async () => {
      const customOptions = [
        { label: 'enabled', value: 'enabled' },
        { label: 'disabled', value: 'disabled', disabled: true },
      ];
      render(<SimpleSelect options={customOptions} />);
      const select = await screen.findByLabelText('Select value');
      selectEvent.openMenu(select);

      expect(await screen.findByRole('option', { name: 'enabled' })).toBeEnabled();
      expect(await screen.findByRole('option', { name: 'disabled' })).toBeEnabled();
    });

    it('should add custom props to input using inputProps', async () => {
      const inputProps = { id: 'myId' };
      render(<SimpleSelect inputProps={inputProps} />);

      const select = await screen.findByLabelText('Select value');

      expect(select).toHaveAttribute('id', 'myId');
    });
  });
});
