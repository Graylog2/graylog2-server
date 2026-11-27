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
import { Field, getIn, useFormikContext } from 'formik';

import { Alert, Input } from 'components/bootstrap';
import { ConfirmDialog, ExternalLink, FormikInput, Switch } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import AppConfig from 'util/AppConfig';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import { DEFAULT_FIELD_LIMIT, MAX_FIELD_LIMIT, type FieldLimitErrors } from 'components/indices/helpers/fieldLimit';

type Props = {
  namePrefix?: string;
  errors?: FieldLimitErrors;
  profile?: { id: string | null | undefined; isIlluminate: boolean; keptFieldsCount?: number | null };
};

export const FieldLimitWarning = ({ showRotateHint = true }: { showRotateHint?: boolean }) => (
  <Alert bsStyle="warning">
    More fields per index use more memory and slow down indexing and searches. Raise the limit only while you fix the
    cause of the field growth, and lower it again afterwards.{' '}
    {showRotateHint &&
      'A change applies when the index set rotates next. Use the Rotate action to apply it right away. '}
    <ExternalLink href={DocsHelper.toString(DocsHelper.PAGES.INDEX_MODEL)}>Read the documentation</ExternalLink>
  </Alert>
);

const ProfileSizeWarning = ({ profileId, fieldLimit }: { profileId: string; fieldLimit: number }) => {
  const { data: profile } = useProfile(profileId);
  const mappingCount = profile.customFieldMappings?.length ?? 0;

  if (mappingCount < fieldLimit) {
    return null;
  }

  return (
    <Alert bsStyle="warning">
      The assigned field type profile has {mappingCount} mappings, as many as or more than the limit of {fieldLimit}{' '}
      fields. Indices created with it may reach the limit right away. You can still save.
    </Alert>
  );
};

const KeptFieldsInfo = ({ keptFieldsCount = undefined }: { keptFieldsCount?: number | null }) =>
  (keptFieldsCount ?? 0) > 0 ? (
    <Alert bsStyle="info">
      {keptFieldsCount} fields were kept from the write index and stay searchable. Fields first seen later are not
      searchable.
    </Alert>
  ) : (
    <Alert bsStyle="warning">
      No fields were kept from the write index. Only the fields the Illuminate package lists stay searchable.
    </Alert>
  );

const dynamicMappingHelp = (profile: Props['profile'], checked: boolean) => {
  if (profile?.isIlluminate) {
    return 'The fields the write index has when you save stay searchable. Fields first seen later are not searchable. If a source floods this index set with new fields, fix the source and rotate the index set before you turn dynamic mapping off. A change applies when the index set rotates next. Use the Rotate action to apply it right away.';
  }

  if (profile && !profile.id && !checked) {
    return 'Assign a field type profile to this index set first.';
  }

  return 'Fields that are not in the field type profile are kept in the stored message and show in message details, but they are not searchable and do not count toward the limit. A change applies when the index set rotates next. Use the Rotate action to apply it right away.';
};

const FieldLimitSettings = ({ namePrefix = '', errors = {}, profile = undefined }: Props) => {
  const { values, initialValues, setFieldValue } = useFormikContext();
  const [showDisableConfirm, setShowDisableConfirm] = useState(false);
  const limitName = `${namePrefix}field_limit`;
  const dynamicMappingName = `${namePrefix}dynamic_mapping_disabled`;
  const fieldLimit = getIn(values, limitName);
  const isCloud = AppConfig.isCloud();
  const dynamicMappingSavedOff = !!getIn(initialValues, dynamicMappingName);

  return (
    <>
      {!isCloud && (
        <>
          <FormikInput
            type="number"
            id={limitName}
            name={limitName}
            label="Max field count"
            placeholder={`${DEFAULT_FIELD_LIMIT} (default)`}
            help={`Values from ${DEFAULT_FIELD_LIMIT} to ${MAX_FIELD_LIMIT} are allowed. Leave empty to use the default of ${DEFAULT_FIELD_LIMIT}.`}
            error={errors.field_limit}
          />
          <FieldLimitWarning />
        </>
      )}
      {profile?.id && <ProfileSizeWarning profileId={profile.id} fieldLimit={fieldLimit || DEFAULT_FIELD_LIMIT} />}
      <Field name={dynamicMappingName}>
        {({ field: { name, value } }) => (
          <Input
            id={name}
            label="Disable dynamic index mapping"
            help={dynamicMappingHelp(profile, !!value)}
            error={errors.dynamic_mapping_disabled}>
            <Switch
              id={name}
              name={name}
              aria-label="Disable dynamic index mapping"
              checked={!!value}
              disabled={!!profile && !profile.isIlluminate && !profile.id && !value}
              onChange={(event) => (event.target.checked ? setShowDisableConfirm(true) : setFieldValue(name, false))}
            />
          </Input>
        )}
      </Field>
      <ConfirmDialog
        show={showDisableConfirm}
        title="Disable dynamic index mapping?"
        btnConfirmText="Disable dynamic mapping"
        onCancel={() => setShowDisableConfirm(false)}
        onConfirm={() => {
          setFieldValue(dynamicMappingName, true);
          setShowDisableConfirm(false);
        }}>
        {dynamicMappingHelp(profile, true)}
      </ConfirmDialog>
      {profile?.isIlluminate && dynamicMappingSavedOff && <KeptFieldsInfo keptFieldsCount={profile.keptFieldsCount} />}
    </>
  );
};

export default FieldLimitSettings;
