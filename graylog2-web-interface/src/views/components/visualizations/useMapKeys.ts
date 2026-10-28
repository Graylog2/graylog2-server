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
import { useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';

import type { KeyMapper } from 'views/components/visualizations/TransformKeys';
import StreamsContext from 'contexts/StreamsContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type { Key } from 'views/logic/searchtypes/pivot/PivotHandler';
import { useInputTitleResolver, useNodeTitleResolver } from 'views/hooks/useInputAndNodeTitles';

// Keys are only known when the mapper is called, so they are collected and resolved in the next render.
const useSeenKeys = () => {
  const [keys, setKeys] = useState<Array<string>>([]);
  const seenKeys = useRef(new Set<string>());

  // Runs after every render, as keys are collected during render. Only grows, so it settles.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (seenKeys.current.size > keys.length) {
      setKeys([...seenKeys.current]);
    }
  });

  const addKey = useCallback((key: string) => {
    seenKeys.current.add(key);
  }, []);

  return [keys, addKey] as const;
};

const useMapKeys = (): KeyMapper => {
  const streams = useContext(StreamsContext);
  const streamsMap = useMemo(() => Object.fromEntries(streams?.map((stream) => [stream.id, stream]) ?? []), [streams]);
  const [nodeKeys, addNodeKey] = useSeenKeys();
  const [inputKeys, addInputKey] = useSeenKeys();
  const resolveNodeTitle = useNodeTitleResolver(nodeKeys);
  const resolveInputTitle = useInputTitleResolver(inputKeys);
  const fieldTypes = useContext(FieldTypesContext);
  const currentFields = useMemo(() => fieldTypes?.currentQuery, [fieldTypes?.currentQuery]);

  return useCallback(
    (key: Key, field: string) => {
      const fieldType = currentFields?.find((type) => type.name === field);

      switch (fieldType?.type?.type) {
        case 'node':
          addNodeKey(String(key));

          return resolveNodeTitle(String(key)) ?? key;
        case 'input':
          addInputKey(String(key));

          return resolveInputTitle(String(key)) ?? key;
        case 'streams':
          return streamsMap?.[key]?.title ?? key;
        default:
          return key;
      }
    },
    [addInputKey, addNodeKey, currentFields, resolveInputTitle, resolveNodeTitle, streamsMap],
  );
};

export default useMapKeys;
