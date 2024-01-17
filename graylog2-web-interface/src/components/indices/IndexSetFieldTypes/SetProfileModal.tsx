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
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { Select } from 'components/common';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import useParams from 'routing/useParams';
import useSetIndexSetProfileMutation from 'components/indices/IndexSetFieldTypes/hooks/useSetIndexSetProfileMutation';
import useProfileOptions from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions';
import { IndexSetsActions } from 'stores/indices/IndexSetsStore';

const StyledLabel = styled.h5`
  font-weight: bold;
  margin-bottom: 5px;
`;

type Props = {
  show: boolean,
  onClose: () => void,
  currentProfile: string | null,
}

const StyledSelect = styled(Select)`
  width: 400px;
  margin-bottom: 20px;
`;

const SetProfileModal = ({ show, onClose, currentProfile }: Props) => {
  const { indexSetId } = useParams();
  const [rotated, setRotated] = useState(true);
  const [profile, setProfile] = useState(null);
  const { setIndexSetFieldTypeProfile, isLoading } = useSetIndexSetProfileMutation();
  const { options, isLoading: profileOptionsIsLoading } = useProfileOptions();

  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);
  const onSubmit = useCallback((e: React.FormEvent) => {
    e.preventDefault();

    setIndexSetFieldTypeProfile({ indexSetId, rotated, profileId: profile }).then(() => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.CHANGE_FOR_INDEX_CHANGED, {
        app_pathname: telemetryPathName,
        app_action_value:
            {
              value: 'index-field-type-profile-changed',
              rotated,
            },
      });
    }).then(() => {
      onClose();

      return IndexSetsActions.get(indexSetId);
    });
  }, [setIndexSetFieldTypeProfile, indexSetId, rotated, profile, sendTelemetry, telemetryPathName, onClose]);

  const onCancel = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.CHANGE_FOR_INDEX_CANCELED, { app_pathname: telemetryPathName, app_action_value: 'removed-custom-field-type-closed' });
    onClose();
  }, [onClose, sendTelemetry, telemetryPathName]);

  useEffect(() => {
    setProfile(currentProfile);
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.CHANGE_FOR_INDEX_OPENED, { app_pathname: telemetryPathName, app_action_value: 'removed-custom-field-type-opened' });
  }, [sendTelemetry, telemetryPathName, currentProfile]);

  const onChangeProfile = (newProfile: string) => setProfile(newProfile);

  return (
    <BootstrapModalForm title={<span>Set Profile</span>}
                        submitButtonText="Set Profile"
                        onSubmitForm={onSubmit}
                        onCancel={onCancel}
                        show={show}
                        bsSize="large"
                        submitButtonDisabled={isLoading}>
      <div>
        <Input id="index_set_profile" label="Select profile">
          <StyledSelect inputId="index_set_profile"
                        options={options}
                        value={profile}
                        onChange={onChangeProfile}
                        placeholder="Select profile"
                        disabled={profileOptionsIsLoading}
                        required />
        </Input>
        <StyledLabel>Select Rotation Strategy</StyledLabel>
        <p>
          To see and use field type profile, you have to rotate indices. You can automatically rotate affected indices after submitting this form or do that manually later.
        </p>
        <Input type="checkbox"
               id="rotate"
               name="rotate"
               label="Rotate affected indices after change"
               onChange={() => setRotated((cur: boolean) => !cur)}
               checked={rotated} />
      </div>
    </BootstrapModalForm>
  );
};

export default SetProfileModal;
