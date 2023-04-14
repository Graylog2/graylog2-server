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

import { useMemo } from 'react';

import type { ModalData } from 'views/logic/valueactions/createEventDefinition/types';
import type { Stream } from 'views/stores/StreamsStore';
import { StreamsStore } from 'views/stores/StreamsStore';
import { useStore } from 'stores/connect';

const useModalData = (mappedData) => {
  const normalizedStreams: {[name: string]: Stream} = useStore(StreamsStore, ({ streams }) => streams.reduce((res, stream) => {
    res[stream.id] = { id: stream.id, title: stream.title };

    return res;
  }, {}));

  return useMemo<ModalData>(() => {
    const {
      aggField = '',
      aggFunction = '',
      aggValue = '',
      columnGroupBy,
      rowGroupBy,
      streams,
      lutParameters,
      ...rest
    } = mappedData;
    const res: ModalData = { ...rest };

    if (aggFunction) {
      res.aggCondition = `${aggFunction}(${aggField}): ${aggValue}`;
    }

    if (streams?.length) {
      res.streams = streams.map((id) => normalizedStreams[id].title).join(', ');
    }

    Object.entries({ columnGroupBy, rowGroupBy }).forEach(([key, entireValue]) => {
      if (entireValue) {
        res[key] = entireValue.join(', ');
      }
    });

    if (lutParameters) {
      res.lutParameters = lutParameters.map((param) => param.name).join(', ');
    }

    return res;
  }, [mappedData, normalizedStreams]);
};

export default useModalData;
