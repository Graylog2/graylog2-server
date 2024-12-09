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
import { useState, useMemo } from 'react';
import styled, { css } from 'styled-components';

import useSetupInputMutations from 'components/inputs/InputSetupWizard/hooks/useSetupInputMutations';
import { InputStatesStore } from 'stores/inputs/InputStatesStore';
import { Button, Row, Col } from 'components/bootstrap';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';
import useInputSetupWizardSteps from 'components/inputs/InputSetupWizard//hooks/useInputSetupWizardSteps';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import { checkHasPreviousStep, checkHasNextStep, checkIsNextStepDisabled, getStepConfigOrData } from 'components/inputs/InputSetupWizard/helpers/stepHelper';
import type { RoutingStepData } from 'components/inputs/InputSetupWizard/steps/SetupRoutingStep';
import SourceGenerator from 'logic/pipelines/SourceGenerator';
import type { StreamConfiguration } from 'components/inputs/InputSetupWizard/hooks/useSetupInputMutations';
import ProgressMessage from 'components/inputs/InputSetupWizard/steps/components/ProgressMessage';

const StepCol = styled(Col)(({ theme }) => css`
  padding-left: ${theme.spacings.lg};
  padding-right: ${theme.spacings.lg};
  padding-top: ${theme.spacings.sm};
`);

const DescriptionCol = styled(Col)(({ theme }) => css`
  margin-bottom: ${theme.spacings.md};
`);

const StyledHeading = styled.h3(({ theme }) => css`
  margin-bottom: ${theme.spacings.md};
`);

const ButtonCol = styled(Col)(({ theme }) => css`
  display: flex;
  justify-content: flex-end;
  gap: ${theme.spacings.xs};
  margin-top: ${theme.spacings.lg};
`);

export type ProcessingSteps = 'createStream' | 'startStream' | 'createPipeline' | 'setupRouting' | 'startInput'

const StartInputStep = () => {
  const { goToPreviousStep, goToNextStep, orderedSteps, activeStep, wizardData, stepsConfig } = useInputSetupWizard();
  const { stepsData } = useInputSetupWizardSteps();
  const hasPreviousStep = checkHasPreviousStep(orderedSteps, activeStep);
  const hasNextStep = checkHasNextStep(orderedSteps, activeStep);
  const isNextStepDisabled = checkIsNextStepDisabled(orderedSteps, activeStep, stepsConfig);
  const [startInputStatus, setStartInputStatus] = useState<'NOT_STARTED' | 'RUNNING' | 'SUCCESS' | 'FAILED'>('NOT_STARTED');
  const isRunningOrDone = startInputStatus === 'RUNNING' || startInputStatus === 'SUCCESS';
  const notStartedOrFailed = startInputStatus === 'NOT_STARTED' || startInputStatus === 'FAILED';
  const hasBeenStarted = startInputStatus !== 'NOT_STARTED';

  const {
    createStreamMutation,
    startStreamMutation,
    createPipelineMutation,
    updateRoutingMutation,
  } = useSetupInputMutations();

  const stepMutations = useMemo<{[key in ProcessingSteps]?}>(() => ({
    createStream: createStreamMutation,
    startStream: startStreamMutation,
    createPipeline: createPipelineMutation,
    setupRouting: updateRoutingMutation,
  }), [createStreamMutation, startStreamMutation, createPipelineMutation, updateRoutingMutation]);

  const createPipeline = async (stream: StreamConfiguration) => {
    const pipeline = {
      title: stream.title,
      description: `Pipeline for Stream: ${stream.title} created by the Input Setup Wizard.`,
    };

    const requestPipeline = {
      ...pipeline,
      source: SourceGenerator.generatePipeline({ ...pipeline, stages: [{ stage: 0, rules: [], match: '' }] }),
    };

    return createPipelineMutation.mutateAsync(requestPipeline);
  };

  const startInput = async () => {
    const { input } = wizardData;

    if (!input) return;

    InputStatesStore.start(input)
      .finally(() => {
        setStartInputStatus('SUCCESS');
      });
  };

  const setupInput = async () => {
    const routingStepData = getStepConfigOrData(stepsData, INPUT_WIZARD_STEPS.SETUP_ROUTING) as RoutingStepData;
    const { input } = wizardData;
    const inputId = input?.id;

    if (!inputId || !routingStepData) return;

    switch (routingStepData.streamType) {
      case 'NEW':

        if (routingStepData.shouldCreateNewPipeline) {
          createPipeline(routingStepData.newStream);
        }

        createStreamMutation.mutateAsync(routingStepData.newStream, {
          onSuccess: (response) => {
            startStreamMutation.mutateAsync(response.stream_id);

            updateRoutingMutation.mutateAsync({ input_id: inputId, stream_id: response.stream_id }).finally(() => {
              startInput();
            });
          },
        });

        break;
      case 'EXISTING':
        updateRoutingMutation.mutateAsync({ input_id: inputId, stream_id: routingStepData.streamId }).finally(() => {
          startInput();
        });

        break;
      case 'DEFAULT':
        startInput();
        break;

      default:
        break;
    }
  };

  const handleStart = () => {
    setStartInputStatus('RUNNING');
    setupInput();
  };

  const onNextStep = () => {
    goToNextStep();
  };

  const handleBackClick = () => {
    goToPreviousStep();
  };

  const isInputStartable = () => {
    const routingStepData = getStepConfigOrData(stepsData, INPUT_WIZARD_STEPS.SETUP_ROUTING) as RoutingStepData;

    if (!routingStepData) return false;
    if (routingStepData.newStream || routingStepData.streamId || routingStepData.streamType === 'DEFAULT') return true;

    return false;
  };

  const getProgressEntityName = (stepName) => {
    const mutation = stepMutations[stepName];

    const routingStepData = getStepConfigOrData(stepsData, INPUT_WIZARD_STEPS.SETUP_ROUTING) as RoutingStepData;

    const name = mutation.data?.title ?? mutation.data?.name ?? undefined;

    switch (stepName) {
      case 'createStream':
        return routingStepData?.newStream.title ?? undefined;

      case 'startStream':
        if (routingStepData.streamType === 'NEW') {
          return routingStepData?.newStream.title ?? undefined;
        }

        return name;
      default:
        return name;
    }
  };

  return (
    <Row>
      <StepCol md={12}>
        <Row>
          <DescriptionCol md={12}>
            <p>
              Setup your Input according to your changes and...better description
            </p>
          </DescriptionCol>
        </Row>
        <Row>
          <Col md={12}>
            {hasBeenStarted && (
              <>
                <StyledHeading>Setting up Input...</StyledHeading>
                  {Object.keys(stepMutations).map((stepName) => {
                    const mutation = stepMutations[stepName];
                    if (mutation.isIdle) return null;

                    const name = getProgressEntityName(stepName);

                    return (
                      <ProgressMessage stepName={stepName as ProcessingSteps}
                                       isLoading={mutation.isLoading}
                                       isSuccess={mutation.isSuccess}
                                       name={name}
                                       isError={mutation.isError}
                                       errorMessage={mutation.error} />
                    );
                  })}
                  {startInputStatus && (
                  <ProgressMessage stepName="startInput"
                                   isLoading={false}
                                   isSuccess={startInputStatus === 'SUCCESS'}
                                   isError={startInputStatus === 'FAILED'} />
                  )}
              </>
            )}
            {(notStartedOrFailed) && (
              <>
                {isInputStartable() ? (
                  <Button onClick={handleStart}>Setup Input</Button>
                ) : (
                  <p>Your Input is not ready to be setup yet. Please complete the previous steps.</p>
                )}
              </>
            )}
          </Col>
        </Row>

        {(hasPreviousStep || hasNextStep) && (
        <Row>
          <ButtonCol md={12}>
            {(hasPreviousStep) && (<Button disabled={isRunningOrDone} onClick={handleBackClick}>Back</Button>)}
            {hasNextStep && (<Button disabled={isNextStepDisabled || startInputStatus === 'RUNNING'} onClick={onNextStep} bsStyle="primary">Input Diagnosis</Button>)}
          </ButtonCol>
        </Row>
        )}
      </StepCol>
    </Row>
  );
};

export default StartInputStep;
