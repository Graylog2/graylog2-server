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
import { useMemo, useState } from 'react';
import { styled } from 'styled-components';

import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import MenuItem from 'components/bootstrap/MenuItem';
import IndexSetCustomFieldTypeRemoveModal
  from 'components/indices/IndexSetFieldTypes/IndexSetCustomFieldTypeRemoveModal';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import type { CustomFieldMapping } from 'components/indices/IndexSetFieldTypeProfiles/types';
import hasOverride from 'components/indices/helpers/hasOverride';
import type { IndexSetFieldType } from 'components/indices/IndexSetFieldTypes/types';
import useSelectedEntitiesData from 'components/common/EntityDataTable/hooks/useSelectedEntitiesData';

type Props = {
  indexSetId: string,
  list: ReadonlyArray<IndexSetFieldType>,
}

const StyledMenuItem = styled(MenuItem)`
  pointer-events: all;
`;

const BulkActions = ({ indexSetId, list }: Props) => {
  const { pushWithState } = useHistory();
  const selectedEntitiesData = useSelectedEntitiesData<IndexSetFieldType>(list);
  const [showResetModal, setShowResetModal] = useState<boolean>(false);
  const customFieldMappings: Array<CustomFieldMapping> = selectedEntitiesData.map(({ fieldName, type }) => ({
    field: fieldName,
    type,
  }));
  const toggleResetModal = () => setShowResetModal((cur) => !cur);

  const createNewProfile = () => {
    pushWithState(
      Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.CREATE,
      {
        customFieldMappings,
      },
    );
  };

  const removableFields = useMemo(() => selectedEntitiesData.filter(hasOverride).map(({ fieldName }) => fieldName), [selectedEntitiesData]);

  return (
    <>
      <BulkActionsDropdown>
        <>
          <StyledMenuItem disabled={!removableFields.length} onSelect={toggleResetModal}>Reset
            {!removableFields.length && '(overridden only)'}
          </StyledMenuItem>
          <MenuItem onSelect={createNewProfile}>Create new profile</MenuItem>
        </>
      </BulkActionsDropdown>
      {showResetModal && (
      <IndexSetCustomFieldTypeRemoveModal show
                                          fields={removableFields}
                                          onClose={toggleResetModal}
                                          indexSetIds={[indexSetId]} />
      )}
    </>
  );
};

export default BulkActions;
