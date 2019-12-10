// @flow strict
import React, { useCallback, useState } from 'react';
import { groupBy } from 'lodash';

import DecoratorList from 'views/components/messagelist/decorators/DecoratorList';
import Decorator from 'views/components/messagelist/decorators/Decorator';
import AddDecoratorButton from 'views/components/messagelist/decorators/AddDecoratorButton';
import StreamSelect, { DEFAULT_SEARCH_ID, DEFAULT_STREAM_ID } from './StreamSelect';

const _formatDecorator = (decorator, decorators, decoratorTypes, updateFn) => {
  const typeDefinition = decoratorTypes[decorator.type] || { requested_configuration: {}, name: `Unknown type: ${decorator.type}` };
  const onUpdate = updateFn
    ? (id, updatedDecorator) => updateFn(decorators.map(curDecorator => (curDecorator.id === id ? updatedDecorator : curDecorator)))
    : () => {};
  const onDelete = updateFn
    ? deletedDecoratorId => updateFn(decorators.filter(({ id }) => (id !== deletedDecoratorId)))
    : () => {};
  return ({
    id: decorator.id,
    title: <Decorator key={`decorator-${decorator.id}`}
                      decorator={decorator}
                      decoratorTypes={decoratorTypes}
                      disableMenu={updateFn === undefined}
                      onUpdate={onUpdate}
                      onDelete={onDelete}
                      typeDefinition={typeDefinition} />,
  });
};

type Props = {
  streams: Array<{ title: string, id: string }>,
  decorators: Array<{ id: string, order: number, type: string }>,
  types: { [string]: any },
};

const DecoratorsConfigUpdate = ({ streams, decorators, types }: Props) => {
  const [currentStream, setCurrentStream] = useState(DEFAULT_STREAM_ID);
  const [updatedDecorators, setUpdatedDecorators] = useState(decorators);

  const decoratorsGroupedByStream = groupBy(updatedDecorators, decorator => (decorator.stream || DEFAULT_SEARCH_ID));
  const currentDecorators = decoratorsGroupedByStream[currentStream] || [];
  const sortedDecorators = currentDecorators
    .sort((d1, d2) => d1.order - d2.order);
  const decoratorItems = sortedDecorators.map(decorator => _formatDecorator(decorator, updatedDecorators, types, setUpdatedDecorators));

  const nextOrder = sortedDecorators.reduce((currentMax, decorator) => Math.max(currentMax, decorator.order), 0) + 1;

  const onCreate = useCallback(newDecorator => setUpdatedDecorators([...updatedDecorators, newDecorator]), [updatedDecorators, setUpdatedDecorators]);

  return (
    <React.Fragment>
      <p>Select the stream for which you want to change the set of default decorators.</p>
      <StreamSelect onChange={setCurrentStream} value={currentStream} streams={streams} />

      <p>Select the type to create a new decorator for this stream:</p>
      <AddDecoratorButton stream={currentStream} nextOrder={nextOrder} decoratorTypes={types} onCreate={onCreate} showHelp={false} />

      <p>Use drag and drop to change the execution order of the decorators.</p>

      <DecoratorList decorators={decoratorItems} />
    </React.Fragment>
  );
};

DecoratorsConfigUpdate.propTypes = {};

export default DecoratorsConfigUpdate;
