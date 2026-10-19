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
import type { IndexSet } from 'stores/indices/IndexSetsStore';

export type ProfileTargetIndexSet = Pick<IndexSet, 'id' | 'title' | 'writable' | 'field_type_profile'> & {
  can_have_profile: boolean;
};

export type ProfileAction = 'set' | 'remove';

export type SkippedIndexSet = { indexSet: ProfileTargetIndexSet; reason: string };

const skipReason = (indexSet: ProfileTargetIndexSet, action: ProfileAction) => {
  if (action === 'set') {
    return indexSet.can_have_profile ? undefined : 'Field types of this index set type cannot be changed';
  }

  return indexSet.field_type_profile ? undefined : 'No field type profile set';
};

export const splitProfileTargets = (indexSets: Array<ProfileTargetIndexSet>, action: ProfileAction) => {
  const affected: Array<ProfileTargetIndexSet> = [];
  const skipped: Array<SkippedIndexSet> = [];

  indexSets.forEach((indexSet) => {
    const reason = skipReason(indexSet, action);

    if (reason) {
      skipped.push({ indexSet, reason });
    } else {
      affected.push(indexSet);
    }
  });

  return { affected, skipped };
};
