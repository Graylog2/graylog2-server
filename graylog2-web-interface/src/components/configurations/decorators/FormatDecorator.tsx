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
import * as React from 'react';

import DecoratorSummary from 'views/components/messagelist/decorators/DecoratorSummary';
import type { Decorator, DecoratorType } from 'views/components/messagelist/decorators/Types';

const formatDecorator = (
  decorator: Decorator,
  decorators: Array<Decorator>,
  decoratorTypes: { [string]: DecoratorType },
  updateFn?: (Array<Decorator>) => mixed,
) => {
  const typeDefinition = decoratorTypes[decorator.type] || { requested_configuration: {}, name: `Unknown type: ${decorator.type}` };

  const onUpdate = updateFn
    ? (id, updatedDecorator) => updateFn(decorators.map((curDecorator) => (curDecorator.id === id ? updatedDecorator : curDecorator)))
    : () => {};

  const onDelete = updateFn
    ? (deletedDecoratorId) => updateFn(decorators.filter(({ id }) => (id !== deletedDecoratorId)))
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
