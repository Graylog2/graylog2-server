// @flow strict
import { difference, isEqual } from 'lodash';

import CombinedProvider from 'injection/CombinedProvider';
import type { Decorator } from 'views/components/messagelist/decorators/Types';

const { DecoratorsActions } = CombinedProvider.get('Decorators');

const DecoratorsUpdater = (newDecorators: Array<Decorator>, oldDecorators: Array<Decorator>) => {
  const newDecoratorIds: Array<string> = newDecorators.filter(({ id }) => id !== undefined).map(({ id }) => id).sort();
  const oldDecoratorIds: Array<string> = oldDecorators.map(({ id }) => id).sort();

  const oldDecoratorsById = oldDecorators
    .reduce((prev, cur) => (cur.id ? { ...prev, [cur.id]: cur } : prev), {});
  const newDecoratorsById = newDecorators
    .reduce((prev, cur) => (cur.id ? { ...prev, [cur.id]: cur } : prev), {});

  const createdDecorators = difference(newDecoratorIds, oldDecoratorIds).map(newDecoratorId => newDecoratorsById[newDecoratorId]);
  const updatedDecorators = newDecorators.filter(({ id }) => id)
    .filter(decorator => decorator.id && oldDecoratorsById[decorator.id] && !isEqual(decorator, oldDecoratorsById[decorator.id]));
  const deletedDecoratorIds = difference(oldDecoratorIds, newDecoratorIds);

  return [
    ...createdDecorators.map(({ id, ...newDecorator }) => () => DecoratorsActions.create(newDecorator)),
    ...updatedDecorators.map(updatedDecorator => () => DecoratorsActions.update(updatedDecorator.id, updatedDecorator)),
    ...deletedDecoratorIds.map(deletedID => () => DecoratorsActions.remove(deletedID)),
  ].reduce((prev, cur) => prev.then(() => cur()), Promise.resolve());
};

export default DecoratorsUpdater;
