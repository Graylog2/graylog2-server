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
import React, { useMemo, useCallback } from 'react';

import { Alert } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useSendTelemetryOnMount from 'logic/telemetry/useSendTelemetryOnMount';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useLocation from 'routing/useLocation';
import ProfileForm from 'components/indices/IndexSetFieldTypeProfiles/ProfileForm';
import type { IndexSetFieldTypeProfileForm } from 'components/indices/IndexSetFieldTypeProfiles/types';
import useProfileMutations from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileMutations';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import StringUtils from 'util/StringUtils';

const CreateProfile = () => {
  const sendTelemetry = useSendTelemetry();
  const { createProfile } = useProfileMutations();
  const location = useLocation<{
    customFieldMappings: any;
    name?: string;
    description?: string;
    skippedFieldsCount?: number;
  }>();
  const history = useHistory();
  const initialValues = useMemo<IndexSetFieldTypeProfileForm>(() => {
    const defaultCustomFieldMappings = location?.state?.customFieldMappings;

    if (defaultCustomFieldMappings) {
      return {
        customFieldMappings: defaultCustomFieldMappings,
        name: location.state.name ?? null,
        description: location.state.description ?? null,
      };
    }

    return undefined;
  }, [location?.state?.customFieldMappings, location?.state?.name, location?.state?.description]);

  const onSubmit = useCallback(
    (profile: IndexSetFieldTypeProfileForm) => {
      createProfile(profile).then(() => {
        sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.CREATED, {
          app_action_value: { mappingsQuantity: profile?.customFieldMappings?.length },
        });

        history.push(Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.OVERVIEW);
      });
    },
    [createProfile, history, sendTelemetry],
  );

  useSendTelemetryOnMount(sendTelemetry, TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.NEW_OPENED, {
    app_action_value: 'create-new-index-set-field-type-profile-opened',
  });

  const onCancel = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.NEW_CANCELED, {
      app_action_value: 'create-new-index-set-field-type-profile-canceled',
    });
    history.goBack();
  }, [history, sendTelemetry]);

  const skippedFieldsCount = location?.state?.skippedFieldsCount ?? 0;

  return (
    <>
      {skippedFieldsCount > 0 && (
        <Alert bsStyle="info">
          {skippedFieldsCount} {StringUtils.pluralize(skippedFieldsCount, 'field was', 'fields were')} left out because
          a profile cannot map reserved fields or fields of this type.
        </Alert>
      )}
      <ProfileForm
        initialValues={initialValues}
        onCancel={onCancel}
        submitButtonText="Create profile"
        submitLoadingText="Creating profile..."
        onSubmit={onSubmit}
      />
    </>
  );
};

export default CreateProfile;
