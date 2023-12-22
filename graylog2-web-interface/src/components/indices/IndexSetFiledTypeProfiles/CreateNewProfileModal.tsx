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
import React, { useMemo, useCallback, useEffect } from 'react';

import { Modal } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import ProfileModalForm from 'components/indices/IndexSetFiledTypeProfiles/ProfileModalForm';
import type { IndexSetFieldTypeProfile } from 'components/indices/IndexSetFiledTypeProfiles/types';
import useProfileMutations from 'components/indices/IndexSetFiledTypeProfiles/hooks/useProfileMutations';

type Props = {
  show: boolean,
  onClose: () => void,
}

const CreateNewProfileModal = ({
  show,
  onClose,
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const { createProfile } = useProfileMutations();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);

  const onSubmit = useCallback((profile: IndexSetFieldTypeProfile) => {
    createProfile(profile).then(() => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.CREATED, {
        app_pathname: telemetryPathName,
        app_action_value:
          {},
      });

      onClose();
    });
  }, [createProfile, onClose, sendTelemetry, telemetryPathName]);

  useEffect(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.NEW_OPENED, { app_pathname: telemetryPathName, app_action_value: 'create-new-index-set-field-type-profile-opened' });
  }, [sendTelemetry, telemetryPathName]);

  const onCancel = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_FIELD_VALUE_ACTION.CHANGE_FIELD_TYPE_CLOSED, { app_pathname: telemetryPathName, app_action_value: 'create-new-index-set-field-type-profile-closed' });
    onClose();
  }, [onClose, sendTelemetry, telemetryPathName]);

  return (
    <Modal title="Create new profile"
           onHide={onCancel}
           show={show}>
      <ProfileModalForm onCancel={onCancel} submitButtonText="Create new profile" title="Create new profile" onSubmit={onSubmit} />
    </Modal>
  );
};

export default CreateNewProfileModal;
