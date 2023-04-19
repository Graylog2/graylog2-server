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
import React, { useEffect, useMemo, useState } from 'react';
import groupBy from 'lodash/groupBy';

import { Button } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import Spinner from 'components/common/Spinner';
import { DecoratorsActions } from 'stores/decorators/DecoratorsStore';
import type { Stream } from 'stores/streams/StreamsStore';
import { StreamsActions } from 'stores/streams/StreamsStore';
import UserNotification from 'util/UserNotification';
import DecoratorList from 'views/components/messagelist/decorators/DecoratorList';
import type { Decorator } from 'views/components/messagelist/decorators/Types';
import { defaultCompare } from 'logic/DefaultCompare';

import DecoratorsConfigUpdate from './decorators/DecoratorsConfigUpdate';
import { DEFAULT_SEARCH_ID } from './decorators/StreamSelect';
import DecoratorsUpdater from './decorators/DecoratorsUpdater';
import formatDecorator from './decorators/FormatDecorator';

const DecoratorsConfig = () => {
  const [streams, setStreams] = useState<Array<Stream> | undefined>();
  const streamsMap = useMemo(() => Object.fromEntries(streams?.map((s) => [s.id, s]) ?? []), [streams]);
  const [decorators, setDecorators] = useState<Array<Decorator> | undefined>();
  const [types, setTypes] = useState();
  const [showConfigModal, setShowConfigModal] = useState(false);

  useEffect(() => { StreamsActions.listStreams().then(setStreams); }, [setStreams]);
  useEffect(() => { DecoratorsActions.available().then(setTypes); }, [setTypes]);
  useEffect(() => { DecoratorsActions.list().then(setDecorators); }, [setDecorators]);

  const openModal = () => setShowConfigModal(true);
  const closeModal = () => setShowConfigModal(false);

  if (!streams || !decorators || !types) {
    return <Spinner />;
  }

  const onSave = (newDecorators: Array<Decorator>) => DecoratorsUpdater(newDecorators, decorators)
    .then(
      () => UserNotification.success('Updated decorators configuration.', 'Success!'),
      (error) => UserNotification.error(`Unable to save new decorators: ${error}`, 'Saving decorators failed'),
    )
    .then(DecoratorsActions.list)
    .then(setDecorators)
    .then(closeModal);

  const decoratorsGroupedByStream = groupBy(decorators, (decorator) => (decorator.stream || DEFAULT_SEARCH_ID));

  const decoratorMap = decorators.length > 0
    ? Object.entries(decoratorsGroupedByStream)
      .map(([id, _decorators]) => [
        streamsMap[id]?.title ?? id,
        _decorators.sort((d1, d2) => d1.order - d2.order).map((decorator) => formatDecorator(decorator, _decorators, types)),
      ] as const)
      .sort((entry1, entry2) => defaultCompare(entry1[0], entry2[0]))
      .map(([streamName, _decorators]) => (
        <>
          <dt>{streamName}</dt>
          <dd><DecoratorList decorators={_decorators} disableDragging /></dd>
        </>
      ))
    : <i>No decorators currently configured.</i>;

  return (
    <div>
      <h2>Decorators Configuration</h2>
      <p>These are the currently configured decorators grouped by stream:</p>
      <p>
        {decoratorMap}
      </p>
      <IfPermitted permissions="decorators:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>Edit configuration</Button>
      </IfPermitted>
      <DecoratorsConfigUpdate show={showConfigModal}
                              streams={streams}
                              decorators={decorators}
                              onCancel={closeModal}
                              onSave={onSave}
                              types={types} />
    </div>
  );
};

export default DecoratorsConfig;
