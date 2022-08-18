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
import { PluginStore } from 'graylog-web-plugin/plugin';
import { useState, useEffect } from 'react';

export const isLocalNode = async (nodeId: string) => {
  const forwarderPlugin = PluginStore.exports('forwarder');
  const _isLocalNode = forwarderPlugin?.[0]?.isLocalNode;

  try {
    if (nodeId && _isLocalNode) {
      return _isLocalNode(nodeId);
    }
  } catch (e) {
    // Do nothing
  }

  return true;
};

const useIsLocalNode = (nodeId: string) => {
  const [_isLocalNode, setIsLocalNode] = useState<boolean | undefined>();

  useEffect(() => {
    const checkIsLocalNode = async () => {
      const result = await isLocalNode(nodeId);
      setIsLocalNode(result);
    };

    checkIsLocalNode().catch(() => setIsLocalNode(true));
  }, [nodeId, _isLocalNode]);

  return { isLocalNode: _isLocalNode };
};

export default useIsLocalNode;
