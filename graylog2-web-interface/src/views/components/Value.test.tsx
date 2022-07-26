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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import FieldType from 'views/logic/fieldtypes/FieldType';

import Value from './Value';
import InteractiveContext from './contexts/InteractiveContext';

describe('Value', () => {
  const openActionsMenu = (value) => {
    userEvent.click(screen.getByText(value));
  };

  describe('actions menu title', () => {
    const Component = (props) => <Value {...props} />;

    it('renders without type information but no children', async () => {
      render(<Value field="foo" queryId="someQueryId" value={42} type={FieldType.Unknown} />);

      openActionsMenu('42');

      await screen.findByText('foo = 42');
    });

    it('renders timestamps with a custom component', async () => {
      render(<Component field="foo"
                        queryId="someQueryId"
                        value="2018-10-02T14:45:40Z"
                        render={({ value }) => `The date ${value}`}
                        type={new FieldType('date', [], [])} />);

      openActionsMenu('The date 2018-10-02 16:45:40.000');
      const title = await screen.findByTestId('value-actions-title');

      expect(title).toHaveTextContent('foo = 2018-10-02 16:45:40.000');
    });

    it('renders numeric timestamps with a custom component', async () => {
      render(<Component field="foo"
                        queryId="someQueryId"
                        value={1571302317}
                        render={({ value }) => `The number ${value}`}
                        type={new FieldType('numeric', [], [])} />);

      await screen.findByText('The number 1571302317');
    });

    it('renders booleans as strings', async () => {
      render(<Component field="foo"
                        queryId="someQueryId"
                        value={false}
                        type={new FieldType('boolean', [], [])} />);

      openActionsMenu('false');

      await screen.findByText('foo = false');
    });

    it('renders booleans as strings even if field type is unknown', async () => {
      render(<Component field="foo" queryId="someQueryId" value={false} type={FieldType.Unknown} />);

      openActionsMenu('false');

      await screen.findByText('foo = false');
    });

    it('renders arrays as strings', async () => {
      render(<Component field="foo"
                        queryId="someQueryId"
                        value={[23, 'foo']}
                        type={FieldType.Unknown} />);

      openActionsMenu('[23,"foo"]');

      await screen.findByText('foo = [23,"foo"]');
    });

    it('renders objects as strings', async () => {
      render(<Component field="foo"
                        queryId="someQueryId"
                        value={{ foo: 23 }}
                        type={FieldType.Unknown} />);

      openActionsMenu('{"foo":23}');

      await screen.findByText('foo = {"foo":23}');
    });

    it('truncates values longer than 30 characters', async () => {
      render(<Component field="message"
                        queryId="someQueryId"
                        value="sophon unbound: [84785:0] error: outgoing tcp: connect: Address already in use for 1.0.0.1"
                        type={new FieldType('string', [], [])} />);

      openActionsMenu('sophon unbound: [84785:0] error: outgoing tcp: connect: Address already in use for 1.0.0.1');

      await screen.findByText('message = sophon unbound: [84785:0] e...');
    });
  });

  it.each`
     interactive | value                     | type                                                         | result
     ${true}     | ${42}                     | ${undefined}                                                 | ${'42'}
     ${true}     | ${'2018-10-02T14:45:40Z'} | ${new FieldType('date', [], [])}    | ${'2018-10-02 16:45:40.000'}
     ${true}     | ${false}                  | ${new FieldType('boolean', [], [])} | ${'false'}
     ${true}     | ${[23, 'foo']}            | ${FieldType.Unknown}                                         | ${'[23,"foo"]'}                
     ${true}     | ${{ foo: 23 }}            | ${FieldType.Unknown}                                         | ${'{"foo":23}'}
     ${false}    | ${42}                     | ${undefined}                                                 | ${'42'}
     ${false}    | ${'2018-10-02T14:45:40Z'} | ${new FieldType('date', [], [])}    | ${'2018-10-02 16:45:40.000'}
     ${false}    | ${false}                  | ${new FieldType('boolean', [], [])} | ${'false'}
     ${false}    | ${[23, 'foo']}            | ${FieldType.Unknown}                                         | ${'[23,"foo"]'}
     ${false}    | ${{ foo: 23 }}            | ${FieldType.Unknown}                                         | ${'{"foo":23}'}
  `('verifying that value $value is rendered correctly when interactive=$interactive', async ({ interactive, value, result, type }) => {
    const Component = (props) => (
      <InteractiveContext.Provider value={interactive}>
        <Value {...props} />
      </InteractiveContext.Provider>
    );

    render(<Component field="foo"
                      queryId="someQueryId"
                      value={value}
                      type={type} />);

    await screen.findByText(result);
  });

  describe('handles value action menu depending on interactive context', () => {
    const component = (interactive) => (props) => (
      <InteractiveContext.Provider value={interactive}>
        <Value {...props} />
      </InteractiveContext.Provider>
    );

    it('does not show value actions if interactive context is `false`', async () => {
      const NoninteractiveComponent = component(false);

      render(<NoninteractiveComponent field="foo"
                                      queryId="someQueryId"
                                      value={{ foo: 23 }}
                                      type={FieldType.Unknown} />);

      openActionsMenu('{"foo":23}');

      expect(screen.queryByText('foo = {"foo":23}')).not.toBeInTheDocument();
    });

    it('shows value actions if interactive context is `true`', async () => {
      const InteractiveComponent = component(true);

      render(<InteractiveComponent field="foo"
                                   queryId="someQueryId"
                                   value={{ foo: 23 }}
                                   type={FieldType.Unknown} />);

      openActionsMenu('{"foo":23}');

      await screen.findByText('foo = {"foo":23}');
    });
  });

  const verifyReplacementOfEmptyValues = async ({ value }) => {
    render(<Value type={FieldType.Unknown} field="foo" queryId="someQueryId" value={value} />);

    await screen.findByText(/Empty Value/);
  };

  it.each`
    value
    ${'\u205f'}
    ${''}
    ${' '}
  `('renders (unicode) spaces as `EmptyValue` component', verifyReplacementOfEmptyValues);
});
