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
import { useState, useEffect } from 'react';

import { Message } from 'views/components/messagelist/Types';
import usePluginEntities from 'views/logic/usePluginEntities';

const useIsLocalNode = (message: Message) => {
  const forwarderPlugin = usePluginEntities('forwarder');
  const _isLocalNode = forwarderPlugin?.[0]?.isLocalNode;
  const [isLocalNode, setIsLocalNode] = useState<boolean | undefined>();
  const { fields: { gl2_source_node } } = message;

  useEffect(() => {
    if (gl2_source_node && _isLocalNode) {
      _isLocalNode(gl2_source_node).then(setIsLocalNode, () => setIsLocalNode(true));
    } else {
      setIsLocalNode(true);
    }
  }, [gl2_source_node, _isLocalNode]);

  return { isLocalNode };
};

export default useIsLocalNode;
