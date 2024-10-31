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

import { adjustFormat } from 'util/DateTime';
import type { IndexRange } from 'views/components/searchbar/queryvalidation/types';
import { isSearchingWarmTier } from 'views/components/searchbar/queryvalidation/warmTierValidation';

type Props = {
  warmTierIndices: IndexRange[]
}

const WarmTierErrorMessage = ({ warmTierIndices } : Props) => {
  if (!isSearchingWarmTier(warmTierIndices)) return null;

  const formatTimestamp = (timestamp: number) : string => `${adjustFormat(new Date((timestamp)), 'default')}`;

  const streamsWithTimestamp = () : Array<{name: string, timestamp: number}> => {
    const streamTimestampsList: {[key: string]: Array<number>} = {};

    warmTierIndices.forEach((index) => {
      index.stream_names.forEach((streamName) => {
        if (!streamTimestampsList[streamName]) {
          streamTimestampsList[streamName] = [index.begin];
        } else {
          streamTimestampsList[streamName].push(index.begin);
        }
      });
    });

    return Object.entries(streamTimestampsList).map(([streamName, timestamps]) => {
      const sortedTimestamps = timestamps.sort((a, b) => a - b);
      const oldestTimestamp = sortedTimestamps[0];

      return { name: streamName, timestamp: oldestTimestamp };
    });
  };

  const streamsWithTimestampMap = streamsWithTimestamp();

  if (streamsWithTimestampMap?.length <= 0) return null;

  return (
    <span>
      The selected time range includes data stored in the Warm Tier, which can be slow to retrieve. Data older than the listed timestamp falls within the Warm Tier for that stream:<br />
      {streamsWithTimestampMap.map((streamWithTimestamp) => (
        <><strong>{streamWithTimestamp.name}:</strong> {formatTimestamp(streamWithTimestamp.timestamp)}<br /></>
      ))}
    </span>
  );
};

export default WarmTierErrorMessage;
