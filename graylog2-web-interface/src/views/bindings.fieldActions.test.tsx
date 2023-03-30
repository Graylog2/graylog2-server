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

describe('Views bindings field actions', () => {
  const { fieldActions } = bindings;
  const defaultArguments = {
    queryId: 'query1',
    contexts: {},
    type: FieldType.Unknown,
  };
  const findAction = (type: string): ActionDefinition<{ analysisDisabledFields?: Array<string> }> => fieldActions.find((binding) => binding.type === type);
  const view = createSearch({ queryId: 'query1' });
  const rootState = { view: { view } } as RootState;
  const getState = jest.fn(() => rootState);

  describe('Show top values', () => {
    const action = findAction('aggregate');
    const { isEnabled } = action;

    it('is present', () => {
      expect(action).toBeDefined();
    });

    it('has `isEnabled` condition', () => {
      expect(isEnabled).toBeDefined();
    });

    it('should be disabled for functions', () => {
      expect(isEnabled({ ...defaultArguments, field: 'avg(something)' }, getState))
        .toEqual(false);
    });

    it('should be enabled for fields', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }, getState))
        .toEqual(true);
    });

    it('should be disabled for compound fields', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('string', [Properties.Compound]),
      }, getState))
        .toEqual(false);
    });

    it('should be enabled for compound fields if they are enumerable', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('compound(int,long)', [Properties.Compound, Properties.Enumerable]),
      }, getState))
        .toEqual(true);
    });

    it('should be disabled for decorated fields', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('string', [Properties.Decorated]),
      }, getState))
        .toEqual(false);
    });

    it('should be disabled when field analysis is disabled', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldTypes.STRING(),
        contexts: { analysisDisabledFields: ['something'] },
      }, getState))
        .toEqual(false);
    });
  });

  describe('Statistics', () => {
    const action = findAction('statistics');
    const { isEnabled } = action;

    it('is present', () => {
      expect(action).toBeDefined();
    });

    it('has `isEnabled` condition', () => {
      expect(isEnabled).toBeDefined();
    });

    it('should be disabled for functions', () => {
      expect(isEnabled({ ...defaultArguments, field: 'avg(something)' }, getState))
        .toEqual(false);
    });

    it('should be enabled for fields', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }, getState))
        .toEqual(true);
    });

    it('should be enabled for compound fields', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('string', [Properties.Compound]),
      }, getState))
        .toEqual(true);
    });

    it('should be disabled when field analysis is disabled', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldTypes.STRING(),
        contexts: { analysisDisabledFields: ['something'] },
      }, getState))
        .toEqual(false);
    });
  });

  describe('AddToAllTables', () => {
    const action = findAction('add-to-all-tables');
    const { isEnabled } = action;

    it('is present', () => {
      expect(action).toBeDefined();
    });

    it('has `isEnabled` condition', () => {
      expect(isEnabled).toBeDefined();
    });

    it('should be disabled for functions', () => {
      expect(isEnabled({ ...defaultArguments, field: 'avg(something)' }, getState))
        .toEqual(false);
    });

    it('should be enabled for fields', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }, getState))
        .toEqual(true);
    });

    it('should be enabled for compound fields', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('string', [Properties.Compound]),
      }, getState))
        .toEqual(true);
    });

    it('should be disabled for decorated fields', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('string', [Properties.Decorated]),
      }, getState))
        .toEqual(false);
    });

    it('should be enabled when field analisys is disabled', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldTypes.STRING(),
        contexts: { analysisDisabledFields: ['something'] },
      }, getState))
        .toEqual(true);
    });
  });

  describe('RemoveFromAllTables', () => {
    const action = findAction('remove-from-all-tables');
    const { isEnabled } = action;

    it('is present', () => {
      expect(action).toBeDefined();
    });

    it('has `isEnabled` condition', () => {
      expect(isEnabled).toBeDefined();
    });

    it('should be disabled for functions', () => {
      expect(isEnabled({ ...defaultArguments, field: 'avg(something)' }, getState))
        .toEqual(false);
    });

    it('should be enabled for fields', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }, getState))
        .toEqual(true);
    });

    it('should be enabled for compound fields', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('string', [Properties.Compound]),
      }, getState))
        .toEqual(true);
    });

    it('should be disabled for decorated fields', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('string', [Properties.Decorated]),
      }, getState))
        .toEqual(false);
    });

    it('should be enabled when field analysis is disabled', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldTypes.STRING(),
        contexts: { analysisDisabledFields: ['something'] },
      }, getState))
        .toEqual(true);
    });
  });
});
