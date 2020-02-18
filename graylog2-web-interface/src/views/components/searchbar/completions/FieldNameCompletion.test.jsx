// @flow strict
import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';

import FieldNameCompletion from './FieldNameCompletion';

jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesStore: MockStore(
    'listen',
    ['getInitialState', jest.fn(() => ({ all: [], queryFields: { get: () => [] } }))],
  ),
}));

const _createField = name => ({ name, type: { type: 'string' } });
const dummyFields = ['source', 'message', 'timestamp'].map(_createField);

const _createQueryFields = fields => ({ get: () => fields });
const _createFieldTypesStoreState = fields => ({ all: fields, queryFields: _createQueryFields(fields) });

describe('FieldNameCompletion', () => {
  beforeEach(() => {
    asMock(FieldTypesStore.getInitialState).mockReturnValue(_createFieldTypesStoreState(dummyFields));
  });
  it('returns empty list if inputs are empty', () => {
    asMock(FieldTypesStore.getInitialState).mockReturnValue(_createFieldTypesStoreState([]));

    const completer = new FieldNameCompletion();
    expect(completer.getCompletions(null, null, '')).toEqual([]);
  });

  it('returns matching fields if prefix is present in one field name', () => {
    const completer = new FieldNameCompletion();
    expect(completer.getCompletions(null, null, 'mess').map(result => result.name))
      .toEqual(['message']);
  });

  it('returns matching fields if prefix is present in at least one field name', () => {
    const completer = new FieldNameCompletion();
    expect(completer.getCompletions(null, null, 'e').map(result => result.name))
      .toEqual(['source', 'message', 'timestamp']);
  });

  it('suffixes matching fields with colon', () => {
    const completer = new FieldNameCompletion();
    expect(completer.getCompletions(null, null, 'e').map(result => result.value))
      .toEqual(['source:', 'message:', 'timestamp:']);
  });
});
