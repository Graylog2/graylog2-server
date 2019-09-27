import React, { useContext, useState } from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

import ConfirmLeaveDialog from 'components/common/ConfirmLeaveDialog';
import Wizard from 'components/common/Wizard';

import FormUtils from 'util/FormsUtils.js';
import history from 'util/History';
import Routes from 'routing/Routes';

import StepAuthorize from 'aws/StepAuthorize';
import { StepsContext } from 'aws/context/Steps';
import { FormDataContext } from 'aws/context/FormData';
import { ApiContext } from 'aws/context/Api';
import { SidebarContext } from 'aws/context/Sidebar';

import StepKinesis from './StepKinesis';
import StepHealthCheck from './StepHealthCheck';
import StepReview from './StepReview';
import SidebarPermissions from './SidebarPermissions';

const CloudWatch = ({ route }) => {
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

  const handleStepChange = (nextStep) => {
    setCurrentStep(nextStep);
  };

  const handleEditClick = nextStep => () => {
    setCurrentStep(nextStep);
  };

  const handleFieldUpdate = ({ target }, fieldData) => {
    const id = target.name || target.id;

    let value = FormUtils.getValueFromInput(target);
    if (typeof value === 'string') {
      value = value.trim();
    }

    if (!dirty) {
      setDirty(true);
    }

    setFormData(id, { ...fieldData, value });
  };

  const handleSubmit = () => {
    clearSidebar();
    const nextStep = availableSteps.indexOf(currentStep) + 1;

    if (availableSteps[nextStep]) {
      const key = availableSteps[nextStep];

      setCurrentStep(key);
      setEnabledStep(key);
    } else {
      setLastStep(true);
      history.push(Routes.SYSTEM.INPUTS);
    }
  };

  const wizardSteps = [
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
                              onEditClick={handleEditClick} />),
      disabled: isDisabledStep('review'),
    },
  ];

  if (availableSteps.length === 0) {
    setAvailableStep(wizardSteps.map(step => step.key));
  }

  return (
    <>
      {dirty && !lastStep && <ConfirmLeaveDialog route={route} question="Are you sure? Your new Input will not be created." />}
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

CloudWatch.propTypes = {
  route: PropTypes.object.isRequired,
};

export default withRouter(CloudWatch);
