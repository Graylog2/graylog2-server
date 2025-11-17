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
import { useNavigate } from 'react-router-dom';
import { useFormikContext } from 'formik';

import Routes from 'routing/Routes';
import { Button } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { Row } from 'components/lookup-tables/layout-componets';

type Props = {
  isCreate: boolean;
  stepIds: Array<string>;
  activeStepId: string;
  onStepChange: (newStepId: string) => void;
  isLoading: boolean;
};

function WizardButtons({ isCreate, stepIds, activeStepId, onStepChange, isLoading }: Props) {
  const { submitForm, resetForm, isValid } = useFormikContext();
  const navigate = useNavigate();
  const onFirstStep = React.useMemo(() => stepIds.indexOf(activeStepId) === 0, [stepIds, activeStepId]);
  const onLastStep = React.useMemo(() => stepIds.indexOf(activeStepId) === stepIds.length - 1, [stepIds, activeStepId]);

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

  if (activeStepId === 'summary') {
    return (
      <Row $align="center" $justify="flex-end" $width="100%">
        <Button onClick={onCancel}>Cancel</Button>
        <Button bsStyle="primary" onClick={onSubmit} disabled={!isValid || isLoading}>
          {isLoading ? (
            <Spinner text={`${isCreate ? 'Creating' : 'Updating'} Lookup Table...`} />
          ) : (
            `${isCreate ? 'Create' : 'Update'} Lookup Table`
          )}
        </Button>
      </Row>
    );
  }

  return (
    <Row $align="center" $justify="space-between" $width="100%">
      <Button onClick={onPrev} disabled={onFirstStep}>
        Previous
      </Button>
      <Button onClick={onNext}>Next</Button>
    </Row>
  );
}

export default WizardButtons;
