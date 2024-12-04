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
import { useCallback, useMemo, useState } from 'react';
import { styled } from 'styled-components';

import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import { MenuItem } from 'components/bootstrap';
import IndexSetCustomFieldTypeRemoveModal
  from 'components/indices/IndexSetFieldTypes/IndexSetCustomFieldTypeRemoveModal';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import type { CustomFieldMapping } from 'components/indices/IndexSetFieldTypeProfiles/types';
import hasOverride from 'components/indices/helpers/hasOverride';
import type { IndexSetFieldType } from 'components/indices/IndexSetFieldTypes/types';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type Props = {
  indexSetId: string,
  selectedEntitiesData: Record<string, IndexSetFieldType>
}

const StyledMenuItem = styled(MenuItem)`
  pointer-events: all;
`;

const BulkActions = ({ indexSetId, selectedEntitiesData }: Props) => {
  const { pushWithState } = useHistory();
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);
  const [showResetModal, setShowResetModal] = useState<boolean>(false);
  const customFieldMappings: Array<CustomFieldMapping> = Object.values(selectedEntitiesData).map(({ fieldName, type }) => ({
    field: fieldName,
    type,
  }));

  const toggleResetModal = () => setShowResetModal((cur) => !cur);

  const createNewProfile = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.CREATE_PROFILE_FROM_SELECTED_RAN, {
      app_pathname: telemetryPathName,
      app_action_value:
        {
          value: 'ran-create-profile-from-selected',
          selectedLength: customFieldMappings.length,
        },
    });

    pushWithState(
      Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.CREATE,
      {
        customFieldMappings,
      },
    );
  }, [customFieldMappings, pushWithState, sendTelemetry, telemetryPathName]);

  const removableFields = useMemo(() => Object.values(selectedEntitiesData).filter(hasOverride).map(({ fieldName }) => fieldName), [selectedEntitiesData]);

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
