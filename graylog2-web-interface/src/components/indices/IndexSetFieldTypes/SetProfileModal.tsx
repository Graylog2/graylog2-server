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
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import styled from 'styled-components';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useSendTelemetryOnMount from 'logic/telemetry/useSendTelemetryOnMount';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { ModalSubmit, Select } from 'components/common';
import { Button, Input, Modal } from 'components/bootstrap';
import useSetIndexSetProfileMutation from 'components/indices/IndexSetFieldTypes/hooks/useSetIndexSetProfileMutation';
import useProfileOptions from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions';
import useRemoveProfileFromIndexMutation from 'components/indices/IndexSetFieldTypes/hooks/useRemoveProfileFromIndexMutation';
import type { ProfileChangeResponse } from 'components/indices/IndexSetFieldTypes/types';
import { resultsWithProblems } from 'components/indices/IndexSetFieldTypes/profileChangeResult';

import ProfileDetails from './ProfileDetails';
import ProfileTargetsList from './ProfileTargetsList';
import ProfileChangeErrorAlert from './ProfileChangeErrorAlert';
import { splitProfileTargets } from './profileTargets';
import type { ProfileAction, ProfileTargetIndexSet } from './profileTargets';

const StyledLabel = styled.h5`
  font-weight: bold;
  margin-bottom: 5px;
`;

const StyledSelect = styled(Select)`
  width: 400px;
  margin-bottom: 20px;
`;

type Props = {
  show: boolean;
  onClose: () => void;
  indexSets: Array<ProfileTargetIndexSet>;
  action?: ProfileAction;
  currentProfile?: string | null;
  onChangeApplied?: (response: ProfileChangeResponse) => void;
};

const SetProfileModal = ({
  show,
  onClose,
  indexSets,
  action = 'set',
  currentProfile = null,
  onChangeApplied = undefined,
}: Props) => {
  const { t } = useTranslation('indexsets');
  const [rotated, setRotated] = useState(true);
  const [profile, setProfile] = useState<string | null>(currentProfile);
  const [lastResult, setLastResult] = useState<{
    response: ProfileChangeResponse;
    titles: Record<string, string>;
  } | null>(null);
  const { setIndexSetFieldTypeProfile, isLoading } = useSetIndexSetProfileMutation();
  const { removeProfileFromIndex, isLoading: isProfileRemoving } = useRemoveProfileFromIndexMutation();
  const { options, isLoading: profileOptionsIsLoading } = useProfileOptions();
  const sendTelemetry = useSendTelemetry();

  const { affected, skipped } = splitProfileTargets(indexSets, action);
  const removalTargets = splitProfileTargets(indexSets, 'remove').affected;
  const isSubmitting = isLoading || isProfileRemoving;
  const problems = lastResult ? resultsWithProblems(lastResult.response) : [];

  const handleResponse = (response: ProfileChangeResponse, telemetryValue: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.CHANGE_FOR_INDEX_CHANGED, {
      app_action_value: {
        value: telemetryValue,
        rotated,
      },
    });
    onChangeApplied?.(response);

    if (resultsWithProblems(response).length === 0) {
      onClose();

      return;
    }

    setLastResult({ response, titles: Object.fromEntries(indexSets.map(({ id, title }) => [id, title])) });
  };

  const removeProfile = (targets: Array<ProfileTargetIndexSet>) =>
    removeProfileFromIndex({ indexSetIds: targets.map(({ id }) => id), rotated }).then((response) =>
      handleResponse(response, 'index-field-type-profile-removed'),
    );

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (action === 'remove') {
      removeProfile(affected);

      return;
    }

    setIndexSetFieldTypeProfile({ indexSetIds: affected.map(({ id }) => id), rotated, profileId: profile }).then(
      (response) => handleResponse(response, 'index-field-type-profile-changed'),
    );
  };

  const onCancel = () => {
    if (isSubmitting) {
      return;
    }

    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.CHANGE_FOR_INDEX_CANCELED, {
      app_action_value: 'removed-custom-field-type-closed',
    });
    onClose();
  };

  useSendTelemetryOnMount(
    sendTelemetry,
    TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.CHANGE_FOR_INDEX_OPENED,
    { app_action_value: 'removed-custom-field-type-opened' },
    [currentProfile],
  );

  const isSetAction = action === 'set';
  const title = isSetAction ? t('field_types.set_profile') : t('field_types.remove_profile');
  const submitText = isSetAction ? t('field_types.set_profile_submit') : t('field_types.remove_profile_submit');

  return (
    <Modal onHide={onCancel} show={show}>
      <form onSubmit={onSubmit}>
        <Modal.Header>
          <Modal.Title>
            <span>{title}</span>
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div>
            {problems.length > 0 && <ProfileChangeErrorAlert results={problems} titles={lastResult.titles} />}
            {isSetAction && (
              <>
                <Input id="index_set_profile" label={t('field_types.select_profile')}>
                  <StyledSelect
                    inputId="index_set_profile"
                    options={options}
                    value={profile}
                    onChange={(newProfile: string) => setProfile(newProfile)}
                    placeholder={t('field_types.select_profile_placeholder')}
                    disabled={profileOptionsIsLoading}
                    required
                  />
                </Input>
                {profile && <ProfileDetails profileId={profile} />}
              </>
            )}
            <ProfileTargetsList affected={affected} skipped={skipped} rotated={rotated} />
            <StyledLabel>{t('field_types.select_rotation_strategy')}</StyledLabel>
            <p>{t('field_types.rotate_hint_profile')}</p>
            <Input
              type="checkbox"
              id="rotate"
              name="rotate"
              label={t('field_types.rotate_after_change')}
              onChange={() => setRotated((cur: boolean) => !cur)}
              checked={rotated}
            />
          </div>
        </Modal.Body>
        <Modal.Footer>
          <ModalSubmit
            submitButtonText={submitText}
            submitLoadingText={`${submitText}...`}
            onCancel={onClose}
            submitButtonType="submit"
            disabledSubmit={isSubmitting || affected.length === 0 || (isSetAction && !profile)}
            isAsyncSubmit
            isSubmitting={isSubmitting}
            leftCol={
              isSetAction &&
              currentProfile && (
                <Button onClick={() => removeProfile(removalTargets)} disabled={isSubmitting} bsStyle="danger">
                  {t('field_types.remove_profile_submit')}
                </Button>
              )
            }
          />
        </Modal.Footer>
      </form>
    </Modal>
  );
};

export default SetProfileModal;
