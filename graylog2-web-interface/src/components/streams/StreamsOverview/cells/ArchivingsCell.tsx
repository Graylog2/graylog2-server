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

import type { Stream } from 'stores/streams/StreamsStore';
import { StatusIcon, Tooltip } from 'components/common';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { ARCHIVE_RETENTION_STRATEGY } from 'stores/indices/IndicesStore';

type Props = {
  stream: Stream,
  indexSets: Array<IndexSet>,
}

const ArchivingsCell = ({ stream, indexSets }: Props) => {
  if (stream.is_default || !stream.is_editable) {
    return null;
  }

  const indexSet = indexSets.find((is) => is.id === stream.index_set_id);

  const archivingEnabled = (indexSet?.use_legacy_rotation && indexSet?.retention_strategy_class === ARCHIVE_RETENTION_STRATEGY) || indexSet?.data_tiering?.archive_before_deletion;

  return (
    <Tooltip withArrow position="right" label={`Archiving is ${archivingEnabled ? 'enabled' : 'disabled'}`}>
      <StatusIcon active={archivingEnabled} />
    </Tooltip>
  );
};

export default ArchivingsCell;
