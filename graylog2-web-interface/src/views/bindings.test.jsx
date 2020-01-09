// @flow strict
import FieldType, { FieldTypes, Properties } from 'views/logic/fieldtypes/FieldType';
import bindings from './bindings';
import type { ActionHandlerCondition } from './components/actions/ActionHandler';

describe('Views bindings', () => {
  describe('field actions', () => {
    const { fieldActions, valueActions } = bindings;
    type FieldAction = {
      isEnabled: ActionHandlerCondition,
    };
    const defaultArguments = {
      queryId: 'query1',
      contexts: {},
      type: FieldType.Unknown,
    };
    const findFieldAction = type => fieldActions.find(binding => binding.type === type);
    const findValueAction = type => valueActions.find(binding => binding.type === type);
    describe('Aggregate', () => {
      // $FlowFixMe: We are assuming here it is generally present
      const action: FieldAction = findFieldAction('aggregate');
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
    });
    describe('Statistics', () => {
      // $FlowFixMe: We are assuming here it is generally present
      const action: FieldAction = findFieldAction('statistics');
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
    });
    describe('AddToAllTables', () => {
      // $FlowFixMe: We are assuming here it is generally present
      const action: FieldAction = findFieldAction('add-to-all-tables');
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
    });
    describe('RemoveFromAllTables', () => {
      // $FlowFixMe: We are assuming here it is generally present
      const action: FieldAction = findFieldAction('remove-from-all-tables');
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
    });
    describe.only('CreateExtractor', () => {
      // $FlowFixMe: We are assuming here it is generally present
      const action: FieldAction = findValueAction('create-extractor');
      const { isEnabled } = action;
      const contexts = { message: {} };
      it('is present', () => {
        expect(action).toBeDefined();
      });
      it('has `isEnabled` condition', () => {
        expect(isEnabled).toBeDefined();
      });
      it('should be enabled for fields with a message context', () => {
        expect(isEnabled({ ...defaultArguments, contexts, field: 'something', type: FieldTypes.STRING() }))
          .toEqual(true);
      });
      it('should be disabled for fields without a message context', () => {
        expect(isEnabled({ ...defaultArguments, contexts: {}, field: 'something', type: FieldTypes.STRING() }))
          .toEqual(false);
      });
      it('should be enabled for fields of type string', () => {
        expect(isEnabled({ ...defaultArguments, contexts, field: 'something', type: FieldTypes.STRING() }))
          .toEqual(true);
      });
      it('should be disabled for fields of type number', () => {
        expect(isEnabled({ ...defaultArguments, contexts, field: 'something', type: FieldTypes.INT() }))
          .toEqual(false);
      });
      it('should be enabled for compound fields', () => {
        expect(isEnabled({
          ...defaultArguments,
          contexts,
          field: 'something',
          type: FieldType.create('string', [Properties.Compound]),
        }))
          .toEqual(true);
      });
      it('should be disabled for decorated fields', () => {
        expect(isEnabled({
          ...defaultArguments,
          contexts,
          field: 'something',
          type: FieldType.create('string', [Properties.Decorated]),
        }))
          .toEqual(false);
      });
    });
  });
});
