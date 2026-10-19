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

import React from 'react';
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import { ModalSubmit, Spinner } from 'components/common';
import { Button } from 'components/bootstrap';
import type { EventDefinitionFormControlsProps } from 'components/event-definitions/event-definitions-types';

const SubmitTextRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: ${({ theme }) => theme.spacings.xs};
`;

const EventDefinitionFormControls = ({
  action,
  activeStepIndex,
  onCancel,
  onOpenNextPage,
  onOpenPrevPage,
  onSubmit,
  steps,
  isSubmitting = false,
  disabledSubmit = false,
}: EventDefinitionFormControlsProps) => {
  const { t } = useTranslation('event_definitions');
  const isEditing = action === 'edit';

  // eslint-disable-next-line no-nested-ternary
  const submitText = isSubmitting
    ? isEditing
      ? t('form_controls.updating')
      : t('form_controls.creating')
    : isEditing
      ? t('form_controls.update')
      : t('form_controls.create');

  if (activeStepIndex === steps.length - 1) {
    return (
      <ModalSubmit
        onCancel={onCancel}
        onSubmit={onSubmit}
        disabledSubmit={disabledSubmit || isSubmitting}
        submitButtonText={
          <SubmitTextRow>
            {isSubmitting && <Spinner text="" />}
            {submitText}
          </SubmitTextRow>
        }
      />
    );
  }

  return (
    <div>
      <Button bsStyle="info" onClick={onOpenPrevPage} disabled={activeStepIndex === 0}>
        {t('form_controls.previous')}
      </Button>
      <div className="pull-right">
        <Button bsStyle="info" onClick={onOpenNextPage}>
          {t('common:actions.next')}
        </Button>
      </div>
    </div>
  );
};

export default EventDefinitionFormControls;
