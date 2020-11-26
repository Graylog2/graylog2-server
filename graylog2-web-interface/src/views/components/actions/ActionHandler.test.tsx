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
import { mount } from 'wrappedEnzyme';

import FieldType from 'views/logic/fieldtypes/FieldType';

import { createHandlerFor } from './ActionHandler';
import type {
  ActionComponentProps,
  ActionComponents, ActionDefinition,
  ActionHandler,
} from './ActionHandler';

describe('ActionHandler', () => {
  it('returns the handler for a function-based definition', () => {
    const actionDefinition: ActionDefinition = {
      type: 'dummy-action',
      title: 'Dummy Action',
      handler: () => Promise.resolve(42),
    };

    const result = createHandlerFor(actionDefinition, jest.fn());

    expect(result).toEqual(actionDefinition.handler);
  });

  it('generates a handler from a component-based definition', () => {
    const setState = jest.fn();
    const setActionComponents = jest.fn((fn) => setState(fn({})));
    const actionDefinition: ActionDefinition = {
      type: 'dummy-action',
      title: 'A Dummy Action',
      component: () => <div>Hello world!</div>,
    };
    const handler: ActionHandler = createHandlerFor(actionDefinition, setActionComponents);

    expect(handler).toBeDefined();

    return handler({ queryId: 'foo', field: 'bar', value: 42, type: FieldType.Unknown, contexts: {} })
      .then(() => {
        expect(setActionComponents).toHaveBeenCalled();
        expect(setState).toHaveBeenCalled();

        const state = setState.mock.calls[0][0];

        expect(Object.entries(state)).toHaveLength(1);

        const Component = state[Object.keys(state)[0]];
        const component = mount(Component);

        expect(component).toHaveProp('field', 'bar');
        expect(component).toHaveProp('queryId', 'foo');
        expect(component).toHaveProp('value', 42);
        expect(component).toHaveProp('onClose');
        expect(component).toMatchSnapshot();
      });
  });

  it('supplied onClose removes component from state', () => {
    const setState = jest.fn();
    const setActionComponents = jest.fn((fn) => setState(fn({})));
    const actionDefinition: ActionDefinition = {
      type: 'dummy-action',
      title: 'A Dummy Action',
      component: () => <div>Hello world!</div>,
    };
    const handler: ActionHandler = createHandlerFor(actionDefinition, setActionComponents);

    return handler({ queryId: 'foo', field: 'bar', value: 42, type: FieldType.Unknown, contexts: {} })
      .then(() => {
        const state: ActionComponents = setState.mock.calls[0][0];
        const component: { props: ActionComponentProps } = Object.values(state)[0];
        const { onClose } = component.props;

        onClose();

        expect(setState).toHaveBeenLastCalledWith({});
      });
  });
});
