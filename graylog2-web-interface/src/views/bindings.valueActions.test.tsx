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

import FieldType, { FieldTypes, Properties } from 'views/logic/fieldtypes/FieldType';

import bindings from './bindings';

jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesStore: MockStore(['getInitialState', () => ({ all: {}, queryFields: {} })]),
}));

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(),
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
    },
    type: FieldType.Unknown,
  };
  const findAction = (type) => valueActions.find((binding) => binding.type === type);

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
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }))
        .toEqual(true);
    });

    it('should be disabled for fields without a message context', () => {
      expect(isEnabled({ ...defaultArguments, contexts: {}, field: 'something', type: FieldTypes.STRING() }))
        .toEqual(false);
    });

    it('should be enabled for fields with type string', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }))
        .toEqual(true);
    });

    it('should be enabled for fields with type number', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.INT() }))
        .toEqual(true);
    });

    it('should be enabled for compound fields', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('string', [Properties.Compound]),
      }))
        .toEqual(true);
    });

    it('should be disabled for decorated fields', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('string', [Properties.Decorated]),
      }))
        .toEqual(false);
    });
  });
});
