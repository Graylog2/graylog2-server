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
import { useMemo } from 'react';
import styled from 'styled-components';
import { useNavigate } from 'react-router-dom';
import { useFormikContext } from 'formik';

import Routes from 'routing/Routes';
import useScopePermissions from 'hooks/useScopePermissions';
import { Button } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { Row } from 'components/lookup-tables/layout-componets';
import type { LookupTableType } from 'components/lookup-tables/lookup-table-form';

const ButtonsRow = styled(Row)`
  margin-top: ${({ theme }) => theme.spacings.lg};
`;

type Props = {
  isCreate: boolean;
  stepIds: Array<string>;
  activeStepId: string;
  onStepChange: (newStepId: string) => void;
  isLoading: boolean;
};

function WizardButtons({ isCreate, stepIds, activeStepId, onStepChange, isLoading }: Props) {
  const { values, submitForm, resetForm, isValid } = useFormikContext<LookupTableType>();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(values);
  const navigate = useNavigate();
  const onFirstStep = useMemo(() => stepIds.indexOf(activeStepId) === 0, [stepIds, activeStepId]);
  const onLastStep = useMemo(() => stepIds.indexOf(activeStepId) === stepIds.length - 1, [stepIds, activeStepId]);

  const onPrev = () => {
    if (onFirstStep) return;
    const currentIndex = stepIds.indexOf(activeStepId);
    const newStepId = stepIds[currentIndex - 1];
    onStepChange(newStepId);
  };

  const onNext = () => {
    if (onLastStep) return;
    const currentIndex = stepIds.indexOf(activeStepId);
    const newStepId = stepIds[currentIndex + 1];
    onStepChange(newStepId);
  };

  const onSubmit = () => submitForm();

  const onCancel = () => {
    resetForm();
    navigate(Routes.SYSTEM.LOOKUPTABLES.OVERVIEW);
  };

  const canModify = useMemo(
    () => !values.id || (!loadingScopePermissions && scopePermissions?.is_mutable),
    [values.id, loadingScopePermissions, scopePermissions?.is_mutable],
  );

  if (activeStepId === 'summary') {
    return (
      <ButtonsRow $align="center" $justify="flex-end" $width="100%">
        <Button onClick={onCancel}>Cancel</Button>
        <Button bsStyle="primary" onClick={onSubmit} disabled={!isValid || isLoading || !canModify}>
          {isLoading ? (
            <Row $gap="xs" $align="center">
              <Spinner text="" /> <span>{isCreate ? 'Creating' : 'Updating'} Lookup Table...</span>
            </Row>
          ) : (
            `${isCreate ? 'Create' : 'Update'} Lookup Table`
          )}
        </Button>
      </ButtonsRow>
    );
  }

  return (
    <ButtonsRow $align="center" $justify="space-between" $width="100%">
      <Button onClick={onPrev} disabled={onFirstStep}>
        Previous
      </Button>
      <Button onClick={onNext}>Next</Button>
    </ButtonsRow>
  );
}

export default WizardButtons;
