// @flow strict
import * as React from 'react';
import DecoratorSummary from 'views/components/messagelist/decorators/DecoratorSummary';
import type { Decorator, DecoratorType } from 'views/components/messagelist/decorators/types';

const formatDecorator = (
  decorator: Decorator,
  decorators: Array<Decorator>,
  decoratorTypes: { [string]: DecoratorType },
  updateFn?: (Array<Decorator>) => mixed,
) => {
  const typeDefinition = decoratorTypes[decorator.type] || { requested_configuration: {}, name: `Unknown type: ${decorator.type}` };
  const onUpdate = updateFn
    ? (id, updatedDecorator) => updateFn(decorators.map(curDecorator => (curDecorator.id === id ? updatedDecorator : curDecorator)))
    : () => {};
  const onDelete = updateFn
    ? deletedDecoratorId => updateFn(decorators.filter(({ id }) => (id !== deletedDecoratorId)))
    : () => {};
  const { id, order } = decorator;
  return ({
    id,
    order,
    title: <DecoratorSummary key={`decorator-${id || 'new'}`}
                             decorator={decorator}
                             decoratorTypes={decoratorTypes}
                             disableMenu={updateFn === undefined}
                             onUpdate={onUpdate}
                             onDelete={onDelete}
                             typeDefinition={typeDefinition} />,
  });
};

export default formatDecorator;
