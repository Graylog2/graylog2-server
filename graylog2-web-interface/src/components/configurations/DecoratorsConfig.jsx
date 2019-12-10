// @flow strict
import React, { useCallback, useEffect, useState } from 'react';
import { groupBy } from 'lodash';

import { IfPermitted } from 'components/common';
import { Button } from 'components/graylog';
import Spinner from 'components/common/Spinner';
import CombinedProvider from 'injection/CombinedProvider';
import StreamsStore from 'stores/streams/StreamsStore';

import Decorator from 'views/components/messagelist/decorators/Decorator';
import DecoratorList from 'views/components/messagelist/decorators/DecoratorList';
import DecoratorsConfigUpdate from './decorators/DecoratorsConfigUpdate';
import StreamSelect, { DEFAULT_SEARCH_ID, DEFAULT_STREAM_ID } from './decorators/StreamSelect';
import DecoratorsUpdater from './decorators/DecoratorsUpdater';

const { DecoratorsActions } = CombinedProvider.get('Decorators');

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

const DecoratorsConfig = () => {
  const [streams, setStreams] = useState();
  const [currentStream, setCurrentStream] = useState(DEFAULT_STREAM_ID);
  const [decorators, setDecorators] = useState();
  const [types, setTypes] = useState();
  const [showModal, setShowModal] = useState(false);
  useEffect(() => { StreamsStore.listStreams().then(setStreams); }, [setStreams]);
  useEffect(() => { DecoratorsActions.available().then(setTypes); }, [setTypes]);
  useEffect(() => { DecoratorsActions.list().then(setDecorators); }, [setDecorators]);

  const openModal = useCallback(() => setShowModal(true), [setShowModal]);
  const closeModal = useCallback(() => setShowModal(false), [setShowModal]);

  if (!streams || !decorators || !types) {
    return <Spinner />;
  }

  const onSave = newDecorators => DecoratorsUpdater(newDecorators, decorators)
    .then(DecoratorsActions.list)
    .then(setDecorators)
    .then(closeModal);

  const decoratorsGroupedByStream = groupBy(decorators, decorator => (decorator.stream || DEFAULT_SEARCH_ID));

  const currentDecorators = decoratorsGroupedByStream[currentStream];
  const sortedDecorators = currentDecorators
    .sort((d1, d2) => d1.order - d2.order);
  const readOnlyDecoratorItems = sortedDecorators.map(decorator => _formatDecorator(decorator, currentDecorators, types));

  const streamOptions = streams
    .filter(({ id }) => Object.keys(decoratorsGroupedByStream).includes(id));

  return (
    <div>
      <h2>Decorators Configuration</h2>
      <p>Select the stream for which you want to see the set of default decorators.</p>
      <StreamSelect streams={streamOptions} onChange={setCurrentStream} value={currentStream} />
      <DecoratorList decorators={readOnlyDecoratorItems} disableDragging />
      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>Update</Button>
      </IfPermitted>
      <DecoratorsConfigUpdate show={showModal}
                              streams={streams}
                              decorators={decorators}
                              onCancel={closeModal}
                              onSave={onSave}
                              types={types} />
    </div>
  );
};

DecoratorsConfig.propTypes = {};

export default DecoratorsConfig;
