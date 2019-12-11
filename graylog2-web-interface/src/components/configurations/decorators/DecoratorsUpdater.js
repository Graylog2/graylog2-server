// @flow strict
import { difference, isEqual } from 'lodash';

import CombinedProvider from 'injection/CombinedProvider';
import type { Decorator } from 'views/components/messagelist/decorators/Types';

const { DecoratorsActions } = CombinedProvider.get('Decorators');

const DecoratorsUpdater = (newDecorators: Array<Decorator>, oldDecorators: Array<Decorator>) => {
  const oldDecoratorsById = oldDecorators
    .reduce((prev, cur) => (cur.id ? { ...prev, [cur.id]: cur } : prev), {});
  const createdDecorators = newDecorators.filter(({ id }) => !id);
  const updatedDecorators = newDecorators.filter(({ id }) => id)
    .filter(decorator => decorator.id && !isEqual(decorator, oldDecoratorsById[decorator.id]));
  const deletedDecoratorIds = difference(oldDecorators.map(({ id }) => id).sort(), newDecorators.map(({ id }) => id).sort());

  return [
    ...createdDecorators.map(newDecorator => () => DecoratorsActions.create(newDecorator)),
    ...updatedDecorators.map(updatedDecorator => () => DecoratorsActions.update(updatedDecorator.id, updatedDecorator)),
    ...deletedDecoratorIds.map(deletedID => () => DecoratorsActions.remove(deletedID)),
  ].reduce((prev, cur) => prev.then(() => cur()), Promise.resolve());
};

export default DecoratorsUpdater;
