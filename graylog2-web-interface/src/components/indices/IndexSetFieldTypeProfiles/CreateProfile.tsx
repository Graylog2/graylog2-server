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
import { useNavigate } from 'react-router-dom';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import ProfileForm from 'components/indices/IndexSetFieldTypeProfiles/ProfileForm';
import type {
  IndexSetFieldTypeProfileForm,
} from 'components/indices/IndexSetFieldTypeProfiles/types';
import useProfileMutations from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileMutations';
import Routes from 'routing/Routes';

const CreateProfile = () => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { createProfile } = useProfileMutations();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);

  const onSubmit = useCallback((profile: IndexSetFieldTypeProfileForm) => {
    createProfile(profile).then(() => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.CREATED, {
        app_pathname: telemetryPathName,
        app_action_value: { mappingsQuantity: profile?.customFieldMappings?.length },
      });

      navigate(Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.OVERVIEW);
    });
  }, [createProfile, navigate, sendTelemetry, telemetryPathName]);

  useEffect(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.NEW_OPENED, { app_pathname: telemetryPathName, app_action_value: 'create-new-index-set-field-type-profile-opened' });
  }, [sendTelemetry, telemetryPathName]);

  const onCancel = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.NEW_CANCELED, { app_pathname: telemetryPathName, app_action_value: 'create-new-index-set-field-type-profile-canceled' });
    navigate(Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.OVERVIEW);
  }, [navigate, sendTelemetry, telemetryPathName]);

  return (
    <ProfileForm onCancel={onCancel} submitButtonText="Create profile" submitLoadingText="Creating profile..." onSubmit={onSubmit} />
  );
};

export default CreateProfile;
