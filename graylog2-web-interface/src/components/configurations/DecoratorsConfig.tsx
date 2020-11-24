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
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { groupBy } from 'lodash';

import { IfPermitted } from 'components/common';
import { Button } from 'components/graylog';
import Spinner from 'components/common/Spinner';
import CombinedProvider from 'injection/CombinedProvider';
import StreamsStore, { Stream } from 'stores/streams/StreamsStore';
import UserNotification from 'util/UserNotification';
import DecoratorList from 'views/components/messagelist/decorators/DecoratorList';

import DecoratorsConfigUpdate from './decorators/DecoratorsConfigUpdate';
import StreamSelect, { DEFAULT_SEARCH_ID, DEFAULT_STREAM_ID } from './decorators/StreamSelect';
import DecoratorsUpdater from './decorators/DecoratorsUpdater';
import formatDecorator from './decorators/FormatDecorator';

import BootstrapModalWrapper from '../bootstrap/BootstrapModalWrapper';

const { DecoratorsActions } = CombinedProvider.get('Decorators');

const DecoratorsConfig = () => {
  const [streams, setStreams] = useState<Array<Stream> | undefined>();
  const [currentStream, setCurrentStream] = useState(DEFAULT_STREAM_ID);
  const [decorators, setDecorators] = useState();
  const [types, setTypes] = useState();
  const configModal = useRef<BootstrapModalWrapper>();

  useEffect(() => { StreamsStore.listStreams().then(setStreams); }, [setStreams]);
  useEffect(() => { DecoratorsActions.available().then(setTypes); }, [setTypes]);
  useEffect(() => { DecoratorsActions.list().then(setDecorators); }, [setDecorators]);

  const openModal = useCallback(() => configModal.current && configModal.current.open(), [configModal]);
  const closeModal = useCallback(() => configModal.current && configModal.current.close(), [configModal]);

  if (!streams || !decorators || !types) {
    return <Spinner />;
  }

  const onSave = (newDecorators) => DecoratorsUpdater(newDecorators, decorators)
    .then(
      () => UserNotification.success('Updated decorators configuration.', 'Success!'),
      (error) => UserNotification.error(`Unable to save new decorators: ${error}`, 'Saving decorators failed'),
    )
    .then(DecoratorsActions.list)
    .then(setDecorators)
    .then(closeModal);

  const decoratorsGroupedByStream = groupBy(decorators, (decorator) => (decorator.stream || DEFAULT_SEARCH_ID));

  const currentDecorators = decoratorsGroupedByStream[currentStream] || [];
  const sortedDecorators = currentDecorators
    .sort((d1, d2) => d1.order - d2.order);
  const readOnlyDecoratorItems = sortedDecorators.map((decorator) => formatDecorator(decorator, currentDecorators, types));

  const streamOptions = streams.filter(({ id }) => Object.keys(decoratorsGroupedByStream).includes(id));

  return (
    <div>
      <h2>Decorators Configuration</h2>
      <p>Select the stream for which you want to see the set of default decorators.</p>
      <StreamSelect streams={streamOptions} onChange={setCurrentStream} value={currentStream} />
      <DecoratorList decorators={readOnlyDecoratorItems} disableDragging />
      <IfPermitted permissions="decorators:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>Update</Button>
      </IfPermitted>
      <DecoratorsConfigUpdate ref={configModal}
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
