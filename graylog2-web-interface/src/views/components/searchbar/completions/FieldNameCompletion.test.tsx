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
// @flow strict
import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import * as Immutable from 'immutable';

import { ViewMetaData, ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { FieldTypeMappingsList, FieldTypesStore, FieldTypesStoreState } from 'views/stores/FieldTypesStore';

import FieldNameCompletion from './FieldNameCompletion';

jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesStore: MockStore(
    'listen',
    ['getInitialState', jest.fn(() => ({ all: [], queryFields: { get: () => [] } }))],
  ),
}));

jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: MockStore(
    ['getInitialState', jest.fn(() => ({ activeQuery: 'query1' }))],
  ),
}));

const _createField = (name) => ({ name, type: { type: 'string' } });
const dummyFields = ['source', 'message', 'timestamp'].map(_createField);

const _createQueryFields = (fields) => ({ get: () => fields }) as unknown as Immutable.Map<string, FieldTypeMappingsList>;
const _createFieldTypesStoreState = (fields): FieldTypesStoreState => ({ all: fields, queryFields: _createQueryFields(fields) });

describe('FieldNameCompletion', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(FieldTypesStore.getInitialState).mockReturnValue(_createFieldTypesStoreState(dummyFields));
  });

  it('returns empty list if inputs are empty', () => {
    asMock(FieldTypesStore.getInitialState).mockReturnValue(_createFieldTypesStoreState([]));

    const completer = new FieldNameCompletion([]);

    expect(completer.getCompletions(null, null, '')).toEqual([]);
  });

  it('returns matching fields if prefix is present in one field name', () => {
    const completer = new FieldNameCompletion();

    expect(completer.getCompletions(null, null, 'mess').map((result) => result.name))
      .toEqual(['message']);
  });

  it('returns matching fields if prefix is present in at least one field name', () => {
    const completer = new FieldNameCompletion([]);

    expect(completer.getCompletions(null, null, 'e').map((result) => result.name))
      .toEqual(['source', 'message', 'timestamp']);
  });

  it('suffixes matching fields with colon', () => {
    const completer = new FieldNameCompletion([]);

    expect(completer.getCompletions(null, null, 'e').map((result) => result.value))
      .toEqual(['source:', 'message:', 'timestamp:']);
  });

  it('returns _exist_-operator if matching prefix', () => {
    const completer = new FieldNameCompletion();

    expect(completer.getCompletions(null, null, '_e').map((result) => result.value))
      .toEqual(['_exists_:']);
  });

  it('returns matching fields after _exists_-operator', () => {
    const completer = new FieldNameCompletion();

    expect(completer.getCompletions(null, { type: 'keyword', value: '_exists_:' }, 'e')
      .map((result) => result.name))
      .toEqual(['source', 'message', 'timestamp']);
  });

  it('returns exists operator together with matching fields', () => {
    const completer = new FieldNameCompletion();

    expect(completer.getCompletions(null, null, 'e').map((result) => result.name))
      .toEqual(['_exists_', 'source', 'message', 'timestamp']);
  });

  it('updates its types when `FieldTypesStore` updates', () => {
    const completer = new FieldNameCompletion([]);
    const newFields = ['nf_version', 'nf_proto_name'];
    const callback = asMock(FieldTypesStore.listen).mock.calls[0][0];

    callback(_createFieldTypesStoreState(newFields.map(_createField)));

    expect(completer.getCompletions(null, null, '').map((result) => result.name))
      .toEqual(['nf_version', 'nf_proto_name']);
  });

  describe('considers current query', () => {
    const completionByName = (fieldName, completions) => completions.find(({ name }) => (name === fieldName));

    const queryFields = {
      get: (queryId, _default) => ({
        query1: ['foo'].map(_createField),
        query2: ['bar'].map(_createField),
      }[queryId] || _default),
    };

    const all = Immutable.List(['foo', 'bar'].map(_createField));

    beforeEach(() => {
      asMock(FieldTypesStore.getInitialState).mockReturnValue({ all, queryFields } as FieldTypesStoreState);
    });

    it('scores fields of current query higher', () => {
      const completer = new FieldNameCompletion([]);

      const completions = completer.getCompletions(null, null, '');

      const completion = (fieldName) => completionByName(fieldName, completions);

      expect(completion('foo')?.score).toEqual(12);
      expect(completion('foo')?.meta).not.toMatch('(not in streams)');

      expect(completion('bar')?.score).toEqual(3);
      expect(completion('bar')?.meta).toMatch('(not in streams)');
    });

    it('scores fields of current query higher', () => {
      const completer = new FieldNameCompletion([]);
      const callback = asMock(ViewMetadataStore.listen).mock.calls[0][0];

      callback({ activeQuery: 'query2' } as ViewMetaData);

      const completions = completer.getCompletions(null, null, '');
      const completion = (fieldName) => completionByName(fieldName, completions);

      expect(completion('foo')?.score).toEqual(3);
      expect(completion('foo')?.meta).toMatch('(not in streams)');

      expect(completion('bar')?.score).toEqual(12);
      expect(completion('bar')?.meta).not.toMatch('(not in streams)');
    });
  });
});
