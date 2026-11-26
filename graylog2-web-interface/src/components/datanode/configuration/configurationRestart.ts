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
import type { ConfigurationParameter, ConfigurationValues, ManagedConfiguration } from './types';

export const restartTargetOf = (nodeId: string | null) => (nodeId === null ? 'the Data Nodes' : 'this Data Node');

const hasChangedParameter = (
  parameters: Array<ConfigurationParameter>,
  changes: ConfigurationValues,
  requiresRestart: boolean,
) =>
  parameters.some(
    ({ name, requires_restart }) => Boolean(requires_restart) === requiresRestart && Object.hasOwn(changes, name),
  );

export const hasChangedRestartParameter = (parameters: Array<ConfigurationParameter>, changes: ConfigurationValues) =>
  hasChangedParameter(parameters, changes, true);

export const hasChangedLiveParameter = (parameters: Array<ConfigurationParameter>, changes: ConfigurationValues) =>
  hasChangedParameter(parameters, changes, false);

/**
 * Whether a single Data Node waits for a restart only because of values shared by all Data Nodes. These aren't part
 * of the parameters of a node, so the response only tells through `restart_required`.
 */
export const hasPendingSharedRestart = (configuration: ManagedConfiguration | undefined, nodeId: string | null) =>
  nodeId !== null &&
  !!configuration?.restart_required &&
  !configuration.parameters.some(({ pending_restart }) => pending_restart);

/**
 * Whether saving has to be followed by a restart. On the page of all Data Nodes, `restart_required` also covers values
 * saved for single Data Nodes, which must not trigger a rolling restart of all of them.
 */
export const needsRestartAfterSave = (
  parameters: Array<ConfigurationParameter>,
  changes: ConfigurationValues,
  saved: ManagedConfiguration,
  nodeId: string | null,
) =>
  hasChangedRestartParameter(parameters, changes) ||
  saved.parameters.some(({ pending_restart }) => pending_restart) ||
  hasPendingSharedRestart(saved, nodeId);
