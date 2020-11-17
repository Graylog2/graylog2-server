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
// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

function withPluginEntities<Props, Entities: {}>(
  Component: React.AbstractComponent<Props>,
  entityMapping: Entities,
): React.AbstractComponent<$Diff<Props, $ObjMapi<Entities, <Key: string>(Key) => $ElementType<Props, Key>>>> {
  const entities = Object.entries(entityMapping)
    .map(([targetKey, entityKey]) => ([targetKey, PluginStore.exports(entityKey)]))
    .reduce((prev, [key, value]) => ({ ...prev, [key]: value }), {});

  return (props: Props) => <Component {...entities} {...props} />;
}

export default withPluginEntities;
