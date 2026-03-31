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

import FieldType from 'views/logic/fieldtypes/FieldType';

import { createHandlerFor } from './ActionHandler';
import type { ActionComponentProps, ActionComponents, ActionDefinition } from './ActionHandler';

describe('ActionHandler', () => {
  const executeThunkAction = jest.fn(() => Promise.resolve(undefined));

  it('returns the handler for a function-based definition', () => {
    const actionDefinition: ActionDefinition = {
      type: 'dummy-action',
      title: 'Dummy Action',
      handler: () => Promise.resolve(42),
      resetFocus: false,
    };

    const result = createHandlerFor(executeThunkAction, actionDefinition, jest.fn());

    expect(result).toEqual(actionDefinition.handler);
  });

  it('generates a handler from a component-based definition', () => {
    const setState = jest.fn();
    const setActionComponents = jest.fn((fn) => setState(fn({})));
    const actionDefinition: ActionDefinition = {
      type: 'dummy-action',
      title: 'A Dummy Action',
      component: () => <div>Hello world!</div>,
      resetFocus: false,
    };
    const handler = createHandlerFor(executeThunkAction, actionDefinition, setActionComponents);

    expect(handler).toBeDefined();

    return handler({ field: 'bar', value: 42, type: FieldType.Unknown, contexts: {} }).then(async () => {
      expect(setActionComponents).toHaveBeenCalled();
      expect(setState).toHaveBeenCalled();

      const state = setState.mock.calls[0][0];

      expect(Object.entries(state)).toHaveLength(1);

      const Component = state[Object.keys(state)[0]];
      render(Component);

      await screen.findByText(/hello world/i);
    });
  });

  it('generates a handler from a thunk-based definition', async () => {
    const thunk = jest.fn(() => () => Promise.resolve('done'));
    const actionDefinition: ActionDefinition = {
      type: 'dummy-action',
      title: 'A Dummy Action',
      thunk,
      resetFocus: false,
    };
    const localExecuteThunkAction = jest.fn(() => Promise.resolve('done'));
    const handler = createHandlerFor(localExecuteThunkAction, actionDefinition, jest.fn());
    const args = { field: 'bar', value: 42, type: FieldType.Unknown, contexts: {} };

    await handler(args);

    expect(localExecuteThunkAction).toHaveBeenCalledWith(thunk, args);
  });

  it('throws for thunk-based definitions when no executeThunkAction is provided', () => {
    const actionDefinition: ActionDefinition = {
      type: 'dummy-action',
      title: 'A Dummy Action',
      thunk: jest.fn(() => () => Promise.resolve('done')),
      resetFocus: false,
    };

    expect(() => createHandlerFor(undefined, actionDefinition, jest.fn())).toThrow(
      "Invalid binding for action: A Dummy Action - thunk actions require 'executeThunkAction'.",
    );
  });

  it('supplied onClose removes component from state', () => {
    const setState = jest.fn();
    const setActionComponents = jest.fn((fn) => setState(fn({})));
    const actionDefinition: ActionDefinition = {
      type: 'dummy-action',
      title: 'A Dummy Action',
      component: () => <div>Hello world!</div>,
      resetFocus: false,
    };
    const handler = createHandlerFor(executeThunkAction, actionDefinition, setActionComponents);

    return handler({ field: 'bar', value: 42, type: FieldType.Unknown, contexts: {} }).then(() => {
      const state: ActionComponents = setState.mock.calls[0][0];
      const component: { props: ActionComponentProps } = Object.values(state)[0];
      const { onClose } = component.props;

      onClose();

      expect(setState).toHaveBeenLastCalledWith({});
    });
  });
});
