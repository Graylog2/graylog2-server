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

// Collectors v2 UI Components

// Types
export * from './types';

// Hooks
export * from './hooks';

// Common components
export { default as StatCard } from './common/StatCard';

// Overview components
export { default as CollectorsOverview } from './overview/CollectorsOverview';
export { default as SourcesTable } from './overview/SourcesTable';

// Fleets components
export { default as FleetList } from './fleets/FleetList';
export { default as FleetDetail } from './fleets/FleetDetail';

// Instances components
export { default as InstanceList } from './instances/InstanceList';

// Deployment components
export { default as DeploymentForm } from './deployment/DeploymentForm';
