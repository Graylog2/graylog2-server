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
import { useCallback, useContext, useMemo } from 'react';
import type { PluginExports } from 'graylog-web-plugin/plugin';

import StreamsContext from 'contexts/StreamsContext';
import { useInputTitleResolver, useNodeTitleResolver } from 'views/hooks/useInputAndNodeTitles';
import type { Key } from 'views/logic/searchtypes/pivot/PivotHandler';
import { getPriorityName } from 'logic/alerts/EventDefinitionPriorityEnum';

const useStreamsKeyMapper = () => {
  const streams = useContext(StreamsContext);
  const streamsMap = useMemo(() => Object.fromEntries((streams ?? []).map((stream) => [stream.id, stream])), [streams]);

  return useCallback((key: Key) => streamsMap[key]?.title ?? key, [streamsMap]);
};

const useInputKeyMapper = (keys: Array<string>) => {
  const resolveTitle = useInputTitleResolver(keys);

  return useCallback((key: Key) => resolveTitle(String(key)) ?? key, [resolveTitle]);
};

const useNodeKeyMapper = (keys: Array<string>) => {
  const resolveTitle = useNodeTitleResolver(keys);

  return useCallback((key: Key) => resolveTitle(String(key)) ?? key, [resolveTitle]);
};

const CoreKeyMappers: PluginExports['visualizationKeyMappers'] = [
  { type: 'streams', useKeyMapper: useStreamsKeyMapper },
  { type: 'input', useKeyMapper: useInputKeyMapper },
  { type: 'node', useKeyMapper: useNodeKeyMapper },
  { type: 'priority', useKeyMapper: () => getPriorityName },
];

export default CoreKeyMappers;
