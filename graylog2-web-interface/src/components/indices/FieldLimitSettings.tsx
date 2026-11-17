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
import { Field, getIn, useFormikContext } from 'formik';

import { Alert, Input } from 'components/bootstrap';
import { ExternalLink, FormikInput, Switch } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import { DEFAULT_FIELD_LIMIT, MAX_FIELD_LIMIT, type FieldLimitErrors } from 'components/indices/helpers/fieldLimit';

type Props = {
  namePrefix?: string;
  errors?: FieldLimitErrors;
  profile?: { id: string | null | undefined; isIlluminate: boolean };
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

const dynamicMappingHelp = (profile: Props['profile'], checked: boolean) => {
  if (profile && !profile.id && !checked) {
    return profile.isIlluminate
      ? 'Illuminate index sets cannot have a field type profile, so dynamic index mapping stays on.'
      : 'Assign a field type profile to this index set first.';
  }

  return 'Fields that are not in the field type profile are kept in the stored message and show in message details, but they are not searchable and do not count toward the limit. A change applies when the index set rotates next. Use the Rotate action to apply it right away.';
};

const FieldLimitSettings = ({ namePrefix = '', errors = {}, profile = undefined }: Props) => {
  const { values } = useFormikContext();
  const limitName = `${namePrefix}field_limit`;
  const dynamicMappingName = `${namePrefix}dynamic_mapping_disabled`;
  const fieldLimit = getIn(values, limitName);

  return (
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
      {profile?.id && <ProfileSizeWarning profileId={profile.id} fieldLimit={fieldLimit || DEFAULT_FIELD_LIMIT} />}
      <Field name={dynamicMappingName}>
        {({ field: { name, value }, form: { setFieldValue } }) => (
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
              disabled={!!profile && !profile.id && !value}
              onChange={(event) => setFieldValue(name, event.target.checked)}
            />
          </Input>
        )}
      </Field>
    </>
  );
};

export default FieldLimitSettings;
