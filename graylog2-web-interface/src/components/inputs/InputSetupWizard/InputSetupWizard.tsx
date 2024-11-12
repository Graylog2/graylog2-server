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
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Modal } from 'components/bootstrap';
import { Wizard } from 'components/common';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';

import { TestInputStep } from './steps';

const InputSetupWizard = () => {
  const { activeStep, setActiveStep, show, closeWizard, wizardData } = useInputSetupWizard();
  const [orderedSteps, setOrderedSteps] = useState([]);
  const { category, subcategory } = wizardData;
  const enterpriseSteps = PluginStore.exports('inputSetupWizard').find((plugin) => (!!plugin.steps))?.steps;

  const steps = useMemo(() => {
    const defaultSteps = {
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
    };
    if (enterpriseSteps) return { ...defaultSteps, ...enterpriseSteps };

    return defaultSteps;
  }, [enterpriseSteps]);

  const determineFirstStep = useCallback(() => {
    if (!category || !subcategory) {
      if (steps[INPUT_WIZARD_STEPS.SELECT_CATEGORY]) {
        setActiveStep(INPUT_WIZARD_STEPS.SELECT_CATEGORY);
        setOrderedSteps([INPUT_WIZARD_STEPS.SELECT_CATEGORY]);

        return;
      }

      setActiveStep(INPUT_WIZARD_STEPS.TEST_INPUT);
      setOrderedSteps([INPUT_WIZARD_STEPS.TEST_INPUT]);

      return;
    }

    if (steps[INPUT_WIZARD_STEPS.ACTIVATE_ILLUMINATE]) {
      setActiveStep(INPUT_WIZARD_STEPS.ACTIVATE_ILLUMINATE);
      setOrderedSteps([INPUT_WIZARD_STEPS.ACTIVATE_ILLUMINATE]);

      return;
    }

    setActiveStep(INPUT_WIZARD_STEPS.TEST_INPUT);
    setOrderedSteps([INPUT_WIZARD_STEPS.TEST_INPUT]);
  }, [setActiveStep, category, subcategory, steps]);

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
