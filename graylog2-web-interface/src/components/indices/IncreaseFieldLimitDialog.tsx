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
import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { Alert, Input } from 'components/bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { cycleActiveWriteIndex, saveIndexSetFieldLimit } from 'components/indices/helpers/indexSetMaintenanceActions';
import { FieldLimitWarning } from 'components/indices/FieldLimitSettings';
import { DEFAULT_FIELD_LIMIT, MAX_FIELD_LIMIT, extractFieldLimitErrors } from 'components/indices/helpers/fieldLimit';

type Props = {
  indexSet: IndexSet;
  onClose: () => void;
};

const IncreaseFieldLimitDialog = ({ indexSet, onClose }: Props) => {
  const queryClient = useQueryClient();
  const [fieldLimit, setFieldLimit] = useState<string>(String(indexSet.field_limit ?? DEFAULT_FIELD_LIMIT));
  const [limitSaved, setLimitSaved] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [rotationFailed, setRotationFailed] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const onSubmit = async () => {
    setFieldError(null);
    setRotationFailed(false);
    setIsSubmitting(true);

    try {
      if (!limitSaved) {
        await saveIndexSetFieldLimit(indexSet, Number(fieldLimit));
        setLimitSaved(true);
        queryClient.invalidateQueries({ queryKey: ['indexSet'] });
      }
    } catch (saveError) {
      setFieldError(extractFieldLimitErrors(saveError).field_limit ?? 'The max field count could not be saved.');
      setIsSubmitting(false);

      return;
    }

    try {
      await cycleActiveWriteIndex(indexSet.id);
      onClose();
    } catch (_rotationError) {
      setRotationFailed(true);
    }

    setIsSubmitting(false);
  };

  return (
    <BootstrapModalForm
      show
      title="Raise max field count and rotate"
      submitButtonText={limitSaved ? 'Retry rotation' : 'Save and rotate'}
      submitButtonDisabled={isSubmitting || !!fieldError}
      onCancel={onClose}
      onSubmitForm={onSubmit}>
      <Input
        id="increase-field-limit"
        type="number"
        label="Max field count"
        value={fieldLimit}
        disabled={limitSaved}
        onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
          setFieldLimit(event.target.value);
          setFieldError(null);
        }}
        help={`Values from ${DEFAULT_FIELD_LIMIT} to ${MAX_FIELD_LIMIT} are allowed.`}
        error={fieldError}
        required
        autoFocus
      />
      <FieldLimitWarning showRotateHint={false} />
      <Alert bsStyle="info">
        Confirming saves the new max field count and rotates the active write index of this index set right away. Lower
        the limit again once the cause of the field growth is fixed.
      </Alert>
      {rotationFailed && (
        <Alert bsStyle="danger">
          The max field count is saved, but the index set could not be rotated. Retry the rotation.
        </Alert>
      )}
    </BootstrapModalForm>
  );
};

export default IncreaseFieldLimitDialog;
