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
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';

type IndexGroupId = 'graylog' | 'system' | 'foreign';

type IndexGroupDefinition = {
  id: IndexGroupId;
  shortLabel: string;
  indexLabel: string;
  matches: (index: IncompatibleIndex) => boolean;
};

export type IndicesGroup = Omit<IndexGroupDefinition, 'matches'> & {
  indices: Array<IncompatibleIndex>;
};

const INDEX_GROUPS: Array<IndexGroupDefinition> = [
  {
    id: 'graylog',
    shortLabel: 'Graylog',
    indexLabel: 'Graylog index',
    matches: (index) => index.managed_index && !index.system_index,
  },
  {
    id: 'system',
    shortLabel: 'System',
    indexLabel: 'System index',
    matches: (index) => index.system_index,
  },
  {
    id: 'foreign',
    shortLabel: 'Foreign',
    indexLabel: 'Foreign index',
    matches: (index) => !index.managed_index && !index.system_index,
  },
];

const DEFAULT_GROUP_ID = INDEX_GROUPS[0].id;

export const groupIncompatibleIndices = (indices: Array<IncompatibleIndex>): Array<IndicesGroup> =>
  INDEX_GROUPS.map(({ id, shortLabel, indexLabel, matches }) => ({
    id,
    shortLabel,
    indexLabel,
    indices: indices.filter(matches),
  }));

export const getFirstGroupWithIndices = (groups: Array<IndicesGroup>) =>
  groups.find((group) => group.indices.length > 0)?.id ?? DEFAULT_GROUP_ID;

export const getSelectedGroup = (groups: Array<IndicesGroup>, selectedGroupId: string) =>
  groups.find((group) => group.id === selectedGroupId) ?? groups[0];
