// @flow strict
import FieldType, { FieldTypes, Properties } from 'views/logic/fieldtypes/FieldType';
import bindings from './bindings';
import type { ActionHandlerCondition } from './components/actions/ActionHandler';

describe('Views bindings field actions', () => {
  const { fieldActions } = bindings;
  type FieldAction = {
    isEnabled: ActionHandlerCondition,
  };
  const defaultArguments = {
    queryId: 'query1',
    contexts: {},
    type: FieldType.Unknown,
  };
  const findAction = type => fieldActions.find(binding => binding.type === type);
  describe('Aggregate', () => {
    // $FlowFixMe: We are assuming here it is generally present
    const action: Action = findAction('aggregate');
    const { isEnabled } = action;
    it('is present', () => {
      expect(action).toBeDefined();
    });
    it('has `isEnabled` condition', () => {
      expect(isEnabled).toBeDefined();
    });
    it('should be disabled for functions', () => {
      expect(isEnabled({ ...defaultArguments, field: 'avg(something)' }))
        .toEqual(false);
    });
    it('should be enabled for fields', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }))
        .toEqual(true);
    });
    it('should be disabled for compound fields', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('string', [Properties.Compound]),
      }))
        .toEqual(false);
    });
    it('should be disabled for decorated fields', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldType.create('string', [Properties.Decorated]),
      }))
        .toEqual(false);
    });
    it('should be disabled when field analisys is disabled', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldTypes.STRING(),
        contexts: { analysisDisabledFields: ['something'] },
      }))
        .toEqual(false);
    });
  });
  describe('Statistics', () => {
    // $FlowFixMe: We are assuming here it is generally present
    const action: FieldAction = findAction('statistics');
    const { isEnabled } = action;
    it('is present', () => {
      expect(action).toBeDefined();
    });
    it('has `isEnabled` condition', () => {
      expect(isEnabled).toBeDefined();
    });
    it('should be disabled for functions', () => {
      expect(isEnabled({ ...defaultArguments, field: 'avg(something)' }))
        .toEqual(false);
    });
    it('should be enabled for fields', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }))
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
    it('should be disabled when field analisys is disabled', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldTypes.STRING(),
        contexts: { analysisDisabledFields: ['something'] },
      }))
        .toEqual(false);
    });
  });
  describe('AddToAllTables', () => {
    // $FlowFixMe: We are assuming here it is generally present
    const action: FieldAction = findAction('add-to-all-tables');
    const { isEnabled } = action;
    it('is present', () => {
      expect(action).toBeDefined();
    });
    it('has `isEnabled` condition', () => {
      expect(isEnabled).toBeDefined();
    });
    it('should be disabled for functions', () => {
      expect(isEnabled({ ...defaultArguments, field: 'avg(something)' }))
        .toEqual(false);
    });
    it('should be enabled for fields', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }))
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
    it('should be enabled when field analisys is disabled', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldTypes.STRING(),
        contexts: { analysisDisabledFields: ['something'] },
      }))
        .toEqual(true);
    });
  });
  describe('RemoveFromAllTables', () => {
    // $FlowFixMe: We are assuming here it is generally present
    const action: FieldAction = findAction('remove-from-all-tables');
    const { isEnabled } = action;
    it('is present', () => {
      expect(action).toBeDefined();
    });
    it('has `isEnabled` condition', () => {
      expect(isEnabled).toBeDefined();
    });
    it('should be disabled for functions', () => {
      expect(isEnabled({ ...defaultArguments, field: 'avg(something)' }))
        .toEqual(false);
    });
    it('should be enabled for fields', () => {
      expect(isEnabled({ ...defaultArguments, field: 'something', type: FieldTypes.STRING() }))
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
    it('should be enabled when field analisys is disabled', () => {
      expect(isEnabled({
        ...defaultArguments,
        field: 'something',
        type: FieldTypes.STRING(),
        contexts: { analysisDisabledFields: ['something'] },
      }))
        .toEqual(true);
    });
  });
});
