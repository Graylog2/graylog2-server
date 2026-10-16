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
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { StaticFields } from '@graylog/server-api';

import { BootstrapModalForm, Input } from 'components/bootstrap';
import type { Input as InputType } from 'components/messageloaders/Types';
import UserNotification from 'util/UserNotification';
import { KEY_PREFIX } from 'hooks/usePaginatedInputs';

type Props = {
  input: InputType;
  setShowModal: (show: boolean) => void;
};

const StaticFieldForm = ({ input, setShowModal }: Props) => {
  const { t } = useTranslation('inputs');
  const [fieldName, setFieldName] = useState<string>('');
  const [fieldValue, setFieldValue] = useState<string>('');
  const queryClient = useQueryClient();

  const addStaticField = () => {
    StaticFields.create(input.id, { key: fieldName, value: fieldValue }).then(
      () => {
        UserNotification.success(t('static_fields.add_success', { field: fieldName, input: input.title }));
        setShowModal(false);
        queryClient.invalidateQueries({ queryKey: KEY_PREFIX });
      },
      (error) => {
        UserNotification.error(
          t('static_fields.add_failed', { error }),
          t('static_fields.add_failed_title', { input: input.title }),
        );
      },
    );
  };

  const handleFieldChange = (name: string, event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.target;

    if (name === 'name') {
      setFieldName(value);
    }

    if (name === 'value') {
      setFieldValue(value);
    }
  };

  return (
    <BootstrapModalForm
      show
      title={t('static_fields.add')}
      submitButtonText={t('static_fields.add_field')}
      onCancel={() => {
        setShowModal(false);
      }}
      onSubmitForm={addStaticField}>
      <p>{t('static_fields.description')}</p>
      <Input
        type="text"
        value={fieldName}
        onChange={(event) => {
          handleFieldChange('name', event);
        }}
        id="field-name"
        label={t('static_fields.field_name')}
        required
        pattern="[A-Za-z0-9_]*"
        title={t('static_fields.field_name_title')}
      />
      <Input
        value={fieldValue}
        onChange={(event) => {
          handleFieldChange('value', event);
        }}
        type="text"
        id="field-value"
        label={t('static_fields.field_value')}
        required
      />
    </BootstrapModalForm>
  );
};

export default StaticFieldForm;
