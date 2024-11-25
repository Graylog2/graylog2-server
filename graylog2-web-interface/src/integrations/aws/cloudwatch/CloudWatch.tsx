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
import React, { useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import ConfirmLeaveDialog from 'components/common/ConfirmLeaveDialog';
import Wizard from 'components/common/Wizard';
import { getValueFromInput } from 'util/FormsUtils.js';
import Routes from 'routing/Routes';
import StepAuthorize from 'integrations/aws/StepAuthorize';
import { StepsContext } from 'integrations/aws/context/Steps';
import { FormDataContext } from 'integrations/aws/context/FormData';
import { ApiContext } from 'integrations/aws/context/Api';
import { SidebarContext } from 'integrations/aws/context/Sidebar';
// import useHistory from 'routing/useHistory';

import StepKinesis from './StepKinesis';
import StepHealthCheck from './StepHealthCheck';
import StepReview from './StepReview';
import SidebarPermissions from './SidebarPermissions';

type CloudWatchProps = {
  externalInputSubmit?: boolean;
  onSubmit?: (...args: any[]) => void;
};

const CloudWatch = ({
  externalInputSubmit = false,
  onSubmit,
}: CloudWatchProps) => {
  const {
    availableSteps,
    currentStep,
    isDisabledStep,
    setAvailableStep,
    setCurrentStep,
    setEnabledStep,
  } = useContext(StepsContext);
  const { setFormData } = useContext(FormDataContext);
  const { availableStreams } = useContext(ApiContext);
  const { sidebar, clearSidebar } = useContext(SidebarContext);
  const [dirty, setDirty] = useState(false);
  const [lastStep, setLastStep] = useState(false);
  // const history = useHistory();
  const navigate = useNavigate();

  const handleStepChange = (nextStep) => {
    setCurrentStep(nextStep);
  };

  const wizardSteps = useMemo(() => {
    const handleEditClick = (nextStep) => () => {
      setCurrentStep(nextStep);
    };

    const handleFieldUpdate = ({ target }, fieldData) => {
      const id = target.name || target.id;

      let value = getValueFromInput(target);

      if (typeof value === 'string') {
        value = value.trim();
      }

      if (!dirty) {
        setDirty(true);
      }

      setFormData(id, { ...fieldData, value });
    };

    const handleSubmit = (maybeFormData) => {
      clearSidebar();
      const nextStep = availableSteps.indexOf(currentStep) + 1;

      if (availableSteps[nextStep]) {
        const key = availableSteps[nextStep];

        setCurrentStep(key);
        setEnabledStep(key);
      } else {
        setLastStep(true);

        if (externalInputSubmit) {
          onSubmit(maybeFormData);
        } else {
          // history.push(Routes.SYSTEM.INPUTS);
          navigate(Routes.SYSTEM.INPUTS);
        }
      }
    };

    return [
      {
        key: 'authorize',
        title: 'AWS Kinesis Authorize',
        component: (<StepAuthorize onSubmit={handleSubmit}
                                   onChange={handleFieldUpdate}
                                   sidebarComponent={<SidebarPermissions />} />),
        disabled: isDisabledStep('authorize'),
      },
      {
        key: 'kinesis-setup',
        title: 'AWS Kinesis Setup',
        component: (<StepKinesis onSubmit={handleSubmit}
                                 onChange={handleFieldUpdate}
                                 hasStreams={availableStreams.length > 0} />),
        disabled: isDisabledStep('kinesis-setup'),
      },
      {
        key: 'health-check',
        title: 'AWS CloudWatch Health Check',
        component: (<StepHealthCheck onSubmit={handleSubmit} onChange={handleFieldUpdate} />),
        disabled: isDisabledStep('health-check'),
      },
      {
        key: 'review',
        title: 'AWS Kinesis Review',
        component: (<StepReview onSubmit={handleSubmit}
                                onEditClick={handleEditClick}
                                externalInputSubmit={externalInputSubmit} />),
        disabled: isDisabledStep('review'),
      },
    ];
  }, [isDisabledStep, availableStreams.length, externalInputSubmit, setCurrentStep, dirty, setFormData, clearSidebar, availableSteps, currentStep, setEnabledStep, onSubmit, navigate]);

  useEffect(() => {
    if (availableSteps.length === 0) {
      setAvailableStep(wizardSteps.map((step) => step.key));
    }
  }, [availableSteps, setAvailableStep, wizardSteps]);

  return (
    <>
      {dirty && !lastStep && <ConfirmLeaveDialog question="Are you sure? Your new Input will not be created." />}
      <Wizard steps={wizardSteps}
              activeStep={currentStep}
              onStepChange={handleStepChange}
              horizontal
              justified
              hidePreviousNextButtons>
        {sidebar}
      </Wizard>
    </>
  );
};

export default CloudWatch;
