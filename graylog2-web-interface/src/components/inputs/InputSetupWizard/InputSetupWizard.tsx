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
import { useEffect, useCallback, useMemo, useState } from 'react';

import { Modal } from 'components/bootstrap';
import { Wizard } from 'components/common';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';
import useIlluminateValidityForSubject from 'components/inputs/InputSetupWizard/hooks/useIlluminateValidityForSubject';

import { ActivateIlluminateStep, SelectCategoryStep, TestInputStep } from './steps';

const InputSetupWizard = () => {
  const { activeStep, setActiveStep, show, closeWizard, wizardData } = useInputSetupWizard();
  const [orderedSteps, setOrderedSteps] = useState([]);
  const { category, subcategory } = wizardData;

  const {
    data: dataIlluminateValidty,
    isFetching: isFetchingIlluminateValidty,
    isError: isErrorIlluminateValidty,
  } = useIlluminateValidityForSubject(category, subcategory);

  const steps = useMemo(() => ({
    [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
      key: INPUT_WIZARD_STEPS.SELECT_CATEGORY,
      title: (
        <>
          Select Category
        </>
      ),
      component: (
        <SelectCategoryStep />
      ),
    },
    [INPUT_WIZARD_STEPS.ACTIVATE_ILLUMINATE]: {
      key: INPUT_WIZARD_STEPS.ACTIVATE_ILLUMINATE,
      title: (
        <>
          Activate Illuminate
        </>
      ),
      component: (
        <ActivateIlluminateStep />
      ),
    },
    [INPUT_WIZARD_STEPS.TEST_INPUT]: {
      key: INPUT_WIZARD_STEPS.TEST_INPUT,
      title: (
        <>
          Test Input
        </>
      ),
      component: (
        <TestInputStep />
      ),
    },
  }), []);

  const determineFirstStep = useCallback(() => {
    if (!category || !subcategory) {
      setActiveStep(INPUT_WIZARD_STEPS.SELECT_CATEGORY);
      setOrderedSteps([INPUT_WIZARD_STEPS.SELECT_CATEGORY]);

      return;
    }

    if (isFetchingIlluminateValidty || isErrorIlluminateValidty || !dataIlluminateValidty?.category_valid || !dataIlluminateValidty.subcategory_valid) {
      setActiveStep(INPUT_WIZARD_STEPS.TEST_INPUT);
      setOrderedSteps([INPUT_WIZARD_STEPS.TEST_INPUT]);

      return;
    }

    setActiveStep(INPUT_WIZARD_STEPS.ACTIVATE_ILLUMINATE);
    setOrderedSteps([INPUT_WIZARD_STEPS.ACTIVATE_ILLUMINATE]);
  }, [setActiveStep, category, subcategory, dataIlluminateValidty, isFetchingIlluminateValidty, isErrorIlluminateValidty]);

  useEffect(() => {
    if (!activeStep) {
      determineFirstStep();
    }

    if (activeStep && orderedSteps.length < 1) {
      setOrderedSteps([activeStep]);
    }
  }, [activeStep, determineFirstStep, orderedSteps]);

  if (!show || orderedSteps.length < 1) return null;

  return (
    <Modal show onHide={closeWizard}>
      <Modal.Body>
        <Wizard activeStep={activeStep}
                hidePreviousNextButtons
                horizontal
                justified
                onStepChange={setActiveStep}
                steps={orderedSteps.map((step) => steps[step])} />
      </Modal.Body>
    </Modal>
  );
};

export default InputSetupWizard;
