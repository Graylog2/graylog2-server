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

import useCurrentUser from 'hooks/useCurrentUser';
import { hasEntityOwnership } from 'hooks/useHasEntityOwnership';
import { createGRN } from 'logic/permissions/GRN';
import type { ExpandedSectionRenderers } from 'components/common/EntityDataTable/types';

import SharedWithOthersSection from './SharedWithOthersSection';
import { SHARED_WITH_OTHERS_ATTRIBUTE, SHARED_WITH_OTHERS_ID } from './Constants';

/**
 * Returns the "Shared With Others" row section while its column is visible.
 * Only owners and admins can open the section, like the share dialog.
 */
const useSharedWithOthersSection = <Entity extends { id: string }>(
  entityType: string,
  entityTypeTitle: string,
  isColumnVisible: boolean,
): ExpandedSectionRenderers<Entity> => {
  const currentUser = useCurrentUser();

  if (!isColumnVisible) {
    return {};
  }

  return {
    [SHARED_WITH_OTHERS_ID]: {
      title: SHARED_WITH_OTHERS_ATTRIBUTE.title,
      content: (entity: Entity) => (
        <SharedWithOthersSection grn={createGRN(entityType, entity.id)} entityTypeTitle={entityTypeTitle} />
      ),
      isAvailable: (entity: Entity) => hasEntityOwnership(currentUser, entityType, entity.id),
    },
  };
};

export default useSharedWithOthersSection;
