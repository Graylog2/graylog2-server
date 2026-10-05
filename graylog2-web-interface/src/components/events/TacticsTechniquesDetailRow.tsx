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

import usePluginEntities from 'hooks/usePluginEntities';

type Entity = {
  tactics_techniques?: string[];
  fields?: Record<string, unknown>;
};

type Props = {
  entity: Entity;
};

// Tactics/Techniques are a Security app concept, so the actual row is owned by a plugin
// (`events.components.tacticsTechniquesDetailRow`, gated by license/feature via `useCondition`).
// This component just does the plugin lookup so every consumer of event details doesn't have to.
const TacticsTechniquesDetailRow = ({ entity }: Props) => {
  const plugin = usePluginEntities('events.components.tacticsTechniquesDetailRow')[0];
  const enabled = plugin?.useCondition?.() ?? !!plugin;

  if (!plugin || !enabled) return null;

  const Row = plugin.component;

  return <Row entity={entity} />;
};

export default TacticsTechniquesDetailRow;
