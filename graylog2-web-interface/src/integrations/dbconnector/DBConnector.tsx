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
import React, { useContext, useState, useEffect, useMemo } from "react";

import { getValueFromInput } from 'util/FormsUtils';
import NumberUtils from 'util/NumberUtils';
import ConfirmLeaveDialog from 'components/common/ConfirmLeaveDialog';
import Wizard from 'components/common/Wizard';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';

import StepSubscribe from './StepSubscribe';
import StepReview from './StepReview';
import StepAuthorize from './StepAuthorize';

import { StepsContext } from '../common/context/Steps';
import { SidebarContext } from '../common/context/Sidebar';
import { FormDataContext } from '../common/context/FormData';
import type {
  WizardStep,
  HandleFieldUpdateType,
  FormDataContextType,
  SidebarContextType,
  StepsContextType,
  HandleSubmitType,
  FormDataType,
} from '../common/utils/types';

interface Props {
  externalInputSubmit?: boolean;
  onSubmit: (formData: FormDataType) => void;
}

const DBConnector = ({ externalInputSubmit = false, onSubmit }: Props) => {
  const {
    availableSteps,
    currentStep,
    isDisabledStep,
    setAvailableStep,
    setCurrentStep,
    setEnabledStep,
  } = useContext<StepsContextType>(StepsContext);

  const { setFormData } = useContext<FormDataContextType>(FormDataContext);
  const { sidebar, clearSidebar } =
    useContext<SidebarContextType>(SidebarContext);
  const [dirty, setDirty] = useState<boolean>(false);
  const [lastStep, setLastStep] = useState<boolean>(false);
  const history = useHistory();

  const wizardSteps: WizardStep[] = useMemo(() => {
    const handleFieldUpdate: HandleFieldUpdateType = (
      { target },
      fieldData
    ) => {
      const id = target.name || target.id;
      let value = getValueFromInput(target);
      if (typeof value === "string") {
        value = value.trim();
      }

      if (!dirty) {
        setDirty(true);
      }
      if (target.type === "number" && NumberUtils.isNumber(value)) {
        setFormData(id, { ...fieldData, value });
        return
      }

      setFormData(id, { ...fieldData, value });
    };

    const handleSubmit: HandleSubmitType = (maybeFormData) => {
      clearSidebar();
      const nextStep = availableSteps.indexOf(currentStep) + 1;

      if (availableSteps[nextStep]) {
        const key = availableSteps[nextStep];

        setEnabledStep(key);
        setCurrentStep(key);
      } else {
        setLastStep(true);

        if (externalInputSubmit) {
          const formData = maybeFormData || {}; // maybeFormData should always be passed if externalInputSubmit is set.
          onSubmit(formData);
        } else {
          history.push(Routes.SYSTEM.INPUTS);
        }
      }
    };

    return [
      {
        key: "authorize",
        title: <>DBConnector Connection Configuration</>,
        component: (
          <StepAuthorize onSubmit={handleSubmit} onChange={handleFieldUpdate} />
        ),
        disabled: isDisabledStep("authorize"),
      },
      {
        key: "subscribe",
        title: <>DBConnector Input Configuration</>,
        component: (
          <StepSubscribe onSubmit={handleSubmit} onChange={handleFieldUpdate} />
        ),
        disabled: isDisabledStep("subscribe"),
      },
      {
        key: "review",
        title: <>DBConnector Final Review</>,
        component: (
          <StepReview
            onSubmit={handleSubmit}
            externalInputSubmit={externalInputSubmit}
          />
        ),
        disabled: isDisabledStep("review"),
      },
    ];
  }, [
    availableSteps,
    clearSidebar,
    currentStep,
    dirty,
    isDisabledStep,
    setCurrentStep,
    setEnabledStep,
    setFormData,
    externalInputSubmit,
  ]);

  useEffect(() => {
    if (availableSteps.length === 0) {
      setAvailableStep(wizardSteps.map((step) => step.key));
    }
  }, [availableSteps, setAvailableStep, wizardSteps]);

  return (
    <>
      {dirty && !lastStep && (
        <ConfirmLeaveDialog question="Are you sure? Your new Input will not be created." />
      )}
      <Wizard
        steps={wizardSteps}
        activeStep={currentStep}
        onStepChange={setCurrentStep}
        horizontal
        justified
        hidePreviousNextButtons
      >
        {sidebar}
      </Wizard>
    </>
  );
};

export default DBConnector;