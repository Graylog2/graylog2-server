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

import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { Stream } from 'stores/streams/StreamsStore';
import useSingleIndexSet from 'components/indices/hooks/useSingleIndexSet';

import DestinationIndexSetSection from './routing-destination/DestinationIndexSetSection';

type Props = {
  stream: Stream;
};

const StreamDataRoutingDestinations = ({ stream }: Props) => {
  const { index_set_id: indexSetId } = stream;
  const { data: indexSet, isSuccess } = useSingleIndexSet(indexSetId);
  const StreamDataWarehouseComponent = PluginStore.exports('dataWarehouse')?.[0]?.StreamDataWarehouse;

  return (
    <>
      {isSuccess && <DestinationIndexSetSection indexSet={indexSet} stream={stream} />}
      <StreamDataWarehouseComponent />
    </>
  );
};

export default StreamDataRoutingDestinations;
