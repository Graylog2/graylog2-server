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
import type { OutdatedIndex } from 'components/indices/hooks/useOutdatedIndices';

// TODO: REMOVE — flip to false (or delete) once real outdated indices are reachable in dev.
const USE_MOCK_OUTDATED_INDICES_FOR_UI_DEV = true;

const MOCK_VERSIONS = ['7.10.2', '6.8.23', '1.3.18'];
const mockVersion = (i: number) => MOCK_VERSIONS[i % MOCK_VERSIONS.length];

const mockOutdatedIndices: Array<OutdatedIndex> = [
  ...Array.from({ length: 20 }, (_, i) => ({
    index_name: `graylog_${i}`,
    version: mockVersion(i),
    warm_index: i % 4 === 0,
    managed_index: true,
    system_index: false,
  })),
  ...Array.from({ length: 21 }, (_, i) => ({
    index_name: `.system_index_${i}`,
    version: mockVersion(i),
    warm_index: false,
    managed_index: false,
    system_index: true,
  })),
  ...Array.from({ length: 22 }, (_, i) => ({
    index_name: `legacy_unknown_${i}`,
    version: mockVersion(i),
    warm_index: false,
    managed_index: false,
    system_index: false,
  })),
];

export default mockOutdatedIndices;

export const outdatedIndicesMockOverride: Array<OutdatedIndex> | undefined = USE_MOCK_OUTDATED_INDICES_FOR_UI_DEV
  ? mockOutdatedIndices
  : undefined;
