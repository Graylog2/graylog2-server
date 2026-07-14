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

import { groupIncompatibleIndices, getFirstGroupWithIndices, getSelectedGroup } from './incompatibleIndexGroups';

const makeIndex = (overrides: Partial<IncompatibleIndex>): IncompatibleIndex => ({
  index_name: 'index',
  version: '2.0.0',
  warm_index: false,
  managed_index: false,
  system_index: false,
  active_write_index: null,
  ...overrides,
});

const graylogIndex = makeIndex({ index_name: 'graylog_0', managed_index: true });
const systemIndex = makeIndex({ index_name: '.system', system_index: true });
const foreignIndex = makeIndex({ index_name: 'legacy' });

describe('incompatibleIndexGroups', () => {
  describe('groupIncompatibleIndices', () => {
    it('returns the three groups in a stable order', () => {
      expect(groupIncompatibleIndices([]).map((group) => group.id)).toEqual(['graylog', 'system', 'foreign']);
    });

    it('classifies each index into its matching group', () => {
      const [graylog, system, foreign] = groupIncompatibleIndices([graylogIndex, systemIndex, foreignIndex]);

      expect(graylog.indices).toEqual([graylogIndex]);
      expect(system.indices).toEqual([systemIndex]);
      expect(foreign.indices).toEqual([foreignIndex]);
    });

    it('treats a managed system index as a system index', () => {
      const managedSystemIndex = makeIndex({ index_name: '.managed-system', managed_index: true, system_index: true });
      const [graylog, system] = groupIncompatibleIndices([managedSystemIndex]);

      expect(graylog.indices).toEqual([]);
      expect(system.indices).toEqual([managedSystemIndex]);
    });
  });

  describe('getFirstGroupWithIndices', () => {
    it('returns the id of the first non-empty group', () => {
      const groups = groupIncompatibleIndices([systemIndex]);

      expect(getFirstGroupWithIndices(groups)).toBe('system');
    });

    it('falls back to the default group when every group is empty', () => {
      const groups = groupIncompatibleIndices([]);

      expect(getFirstGroupWithIndices(groups)).toBe('graylog');
    });
  });

  describe('getSelectedGroup', () => {
    it('returns the group matching the given id', () => {
      const groups = groupIncompatibleIndices([foreignIndex]);

      expect(getSelectedGroup(groups, 'foreign').id).toBe('foreign');
    });

    it('falls back to the first group for an unknown id', () => {
      const groups = groupIncompatibleIndices([]);

      expect(getSelectedGroup(groups, 'does-not-exist').id).toBe('graylog');
    });
  });
});
