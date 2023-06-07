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
import MockStore from 'helpers/mocking/StoreMock';
import MockAction from 'helpers/mocking/MockAction';
import FieldType, { FieldTypes, Properties } from 'views/logic/fieldtypes/FieldType';
import type { ActionDefinition } from 'views/components/actions/ActionHandler';
import { createSearch } from 'fixtures/searches';
import type { RootState } from 'views/types';

import bindings from './bindings';

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(),
  ConfigurationsActions: {
    listSearchesClusterConfig: MockAction(),
  },
}));

jest.mock('stores/decorators/DecoratorsStore', () => ({
  DecoratorsStore: MockStore(),
}));

describe('Views bindings value actions', () => {
  const { valueActions } = bindings;
  const defaultArguments = {
    queryId: 'query1',
    contexts: {
      message: {},
      isLocalNode: true,
    },
    type: FieldType.Unknown,
  };
  const findAction = (type: string): ActionDefinition<{}> => valueActions.find((binding) => binding.type === type);
  const view = createSearch({ queryId: 'query1' });
  const rootState = { view: { view } } as RootState;
  const getState = jest.fn(() => rootState);

  describe('CreateExtractor', () => {
    const action = findAction('create-extractor');
    const { isEnabled } = action;

    it('is present', () => {
      expect(action).toBeDefined();
    });

    it('has `isEnabled` condition', () => {
      expect(isEnabled).toBeDefined();
    });

    it('should be enabled for fields with a message context', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }, getState))
        .toEqual(true);
    });

    it('should be disabled for fields without a message context', () => {
      expect(isEnabled({ ...defaultArguments, contexts: {}, field: 'something', type: FieldTypes.STRING() }, getState))
        .toEqual(false);
    });

    it('should be enabled for fields with type string', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }, getState))
        .toEqual(true);
    });

    it('should be enabled for fields with type number', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.INT() }, getState))
        .toEqual(true);
    });

    it('should be enabled for compound fields', () => {
      expect(
        isEnabled({
          ...defaultArguments,
          field: 'something',
          type: FieldType.create('string', [Properties.Compound]),
        }, getState),
      ).toEqual(true);
    });

    it('should be disabled for decorated fields', () => {
      expect(
        isEnabled({
          ...defaultArguments,
          field: 'something',
          type: FieldType.create('string', [Properties.Decorated]),
        }, getState),
      ).toBe(false);
    });
  });

  describe('Add to query', () => {
    const action = findAction('add-to-query');

    it('is present', () => {
      expect(action).toBeDefined();
    });

    it('has `isEnabled` condition', () => {
      expect(action.isEnabled).toBeDefined();
    });

    it('should be disabled for decorated fields', () => {
      expect(
        action.isEnabled({
          ...defaultArguments,
          field: 'something',
          type: FieldType.create('string', [Properties.Decorated]),
        }, getState),
      ).toEqual(false);
    });

    it('should be disabled for functions', () => {
      expect(action.isEnabled({ ...defaultArguments, field: 'count(something)', type: FieldTypes.STRING() }, getState))
        .toEqual(false);
    });

    it('should be enabled for fields with type string', () => {
      expect(action.isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }, getState))
        .toEqual(true);
    });
  });
});
