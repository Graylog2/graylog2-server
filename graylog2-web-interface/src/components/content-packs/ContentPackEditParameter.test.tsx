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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import ContentPackEditParameter from 'components/content-packs/ContentPackEditParameter';

describe('<ContentPackEditParameter />', () => {
  it('should render with empty parameters', async () => {
    render(<ContentPackEditParameter />);

    await screen.findByRole('heading', { name: /create parameter/i });
  });

  it('should render a form for creation', async () => {
    const parameters = [
      {
        name: 'A parameter name',
        title: 'A parameter title',
        description: 'A parameter descriptions',
        type: 'string',
        default_value: 'test',
      },
    ];
    render(<ContentPackEditParameter parameters={parameters} />);

    await screen.findByRole('heading', { name: /create parameter/i });
  });

  it('should render a form for editing', () => {
    const parameters = [
      {
        name: 'A parameter name',
        title: 'A parameter title',
        description: 'A parameter descriptions',
        type: 'string',
        default_value: 'test',
      },
    ];

    const parameterToEdit = parameters[0];

    render(<ContentPackEditParameter parameters={parameters} parameterToEdit={parameterToEdit} />);

    expect(screen.getByDisplayValue('A parameter name')).toBeInTheDocument();
  });

  it('should create a parameter', async () => {
    const changeFn = jest.fn();

    render(<ContentPackEditParameter onUpdateParameter={changeFn} />);

    await userEvent.type(screen.getByLabelText(/name/i), 'name');
    await userEvent.type(screen.getByLabelText(/title/i), 'title');
    await userEvent.type(screen.getByLabelText(/description/i), 'descr');
    fireEvent.change(screen.getByLabelText(/type/i), { target: { value: 'integer' } });
    await userEvent.type(screen.getByLabelText(/default value/i), '1');

    fireEvent.submit(await screen.findByTestId('parameter-form'));

    expect(changeFn).toHaveBeenCalledWith({
      name: 'name',
      title: 'title',
      description: 'descr',
      type: 'integer',
      default_value: 1,
    });
  });

  it('should not create a parameter if name is missing', async () => {
    const changeFn = jest.fn();

    render(<ContentPackEditParameter onUpdateParameter={changeFn} />);

    await userEvent.type(screen.getByLabelText(/title/i), 'title');
    await userEvent.type(screen.getByLabelText(/description/i), 'descr');
    await userEvent.type(screen.getByLabelText(/default value/i), 'test');

    fireEvent.submit(await screen.findByTestId('parameter-form'));

    expect(changeFn).not.toHaveBeenCalled();
  });

  it('should validate with existing names when editing a parameter', async () => {
    const parameters = [
      { name: 'hans', title: 'hans', description: 'hans' },
      { name: 'franz', title: 'franz', description: 'franz' },
    ];

    render(<ContentPackEditParameter parameters={parameters} parameterToEdit={parameters[0]} />);

    const nameInput = screen.getByLabelText(/name/i);
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, 'franz');

    fireEvent.submit(await screen.findByTestId('parameter-form'));

    expect(screen.getByText(/must be unique/i)).toBeInTheDocument();
  });

  describe('validation', () => {
    beforeEach(async () => {
      const parameters = [{ name: 'hans', title: 'hans', description: 'hans' }];

      // eslint-disable-next-line testing-library/no-render-in-lifecycle
      render(<ContentPackEditParameter parameters={parameters} />);

      await userEvent.type(screen.getByLabelText(/name/i), 'name');
      await userEvent.type(screen.getByLabelText(/title/i), 'title');
      await userEvent.type(screen.getByLabelText(/description/i), 'descr');
    });

    it('should validate the parameter name', async () => {
      const nameInput = screen.getByLabelText(/name/i);
      await userEvent.clear(nameInput);
      await userEvent.type(nameInput, 'hans');
      fireEvent.submit(await screen.findByTestId('parameter-form'));

      expect(screen.getByText(/must be unique/i)).toBeInTheDocument();

      await userEvent.clear(nameInput);
      await userEvent.type(nameInput, 'hans-dampf');
      fireEvent.submit(await screen.findByTestId('parameter-form'));

      expect(screen.getByText(/only contain A-Z, a-z, 0-9 and _/i)).toBeInTheDocument();

      await userEvent.clear(nameInput);
      await userEvent.type(nameInput, 'dampf');
      fireEvent.submit(await screen.findByTestId('parameter-form'));

      expect(screen.getByText(/must not contain a space/i)).toBeInTheDocument();
    });

    it('should validate the parameter input from type double', async () => {
      fireEvent.change(screen.getByLabelText(/type/i), { target: { value: 'double' } });
      await userEvent.type(screen.getByLabelText(/default value/i), 'test');
      fireEvent.submit(await screen.findByTestId('parameter-form'));

      expect(screen.getByText(/not a double value/i)).toBeInTheDocument();

      const input = screen.getByLabelText(/default value/i);
      await userEvent.clear(input);
      await userEvent.type(input, '1.0');
      fireEvent.submit(await screen.findByTestId('parameter-form'));

      expect(screen.getByText(/default value if the parameter is not optional/i)).toBeInTheDocument();
    });

    it('should validate the parameter input from type integer', async () => {
      fireEvent.change(screen.getByLabelText(/type/i), { target: { value: 'integer' } });
      await userEvent.type(screen.getByLabelText(/default value/i), 'test');
      fireEvent.submit(await screen.findByTestId('parameter-form'));

      expect(screen.getByText(/not an integer value/i)).toBeInTheDocument();

      const input = screen.getByLabelText(/default value/i);
      await userEvent.clear(input);
      await userEvent.type(input, '1');
      fireEvent.submit(await screen.findByTestId('parameter-form'));

      expect(screen.getByText(/default value if the parameter is not optional/i)).toBeInTheDocument();
    });

    it('should validate the parameter input from type boolean', async () => {
      fireEvent.change(screen.getByLabelText(/type/i), { target: { value: 'boolean' } });
      await userEvent.type(screen.getByLabelText(/default value/i), 'test');
      fireEvent.submit(await screen.findByTestId('parameter-form'));

      expect(screen.getByText(/must be either true or false/i)).toBeInTheDocument();

      const input = screen.getByLabelText(/default value/i);
      await userEvent.clear(input);
      await userEvent.type(input, 'true');
      fireEvent.submit(await screen.findByTestId('parameter-form'));

      expect(screen.getByText(/default value if the parameter is not optional/i)).toBeInTheDocument();
    });
  });
});
