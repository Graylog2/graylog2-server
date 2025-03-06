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
import { useEffect, useState, useMemo } from 'react';
import type { UseMutationResult } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import Routes from 'routing/Routes';
import useSetupInputMutations from 'components/inputs/InputSetupWizard/hooks/useSetupInputMutations';
import { InputStatesStore } from 'stores/inputs/InputStatesStore';
import { Button, Row, Col } from 'components/bootstrap';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';
import useInputSetupWizardSteps from 'components/inputs/InputSetupWizard//hooks/useInputSetupWizardSteps';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import {
  checkHasPreviousStep,
  checkHasNextStep,
  getStepData,
} from 'components/inputs/InputSetupWizard/helpers/stepHelper';
import type { RoutingStepData } from 'components/inputs/InputSetupWizard/steps/SetupRoutingStep';
import type { StreamConfiguration } from 'components/inputs/InputSetupWizard/hooks/useSetupInputMutations';
import ProgressMessage from 'components/inputs/InputSetupWizard/steps/components/ProgressMessage';

import { StepWrapper, DescriptionCol, ButtonCol, StyledHeading } from './components/StepWrapper';

export type ProcessingSteps =
  | 'createStream'
  | 'startStream'
  | 'createPipeline'
  | 'setupRouting'
  | 'deleteStream'
  | 'deletePipeline'
  | 'deleteRouting'
  | 'result';

const StartInputStep = () => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);
  const navigateTo = useNavigate();
  const { goToPreviousStep, orderedSteps, activeStep, wizardData } = useInputSetupWizard();
  const { stepsData } = useInputSetupWizardSteps();
  const hasPreviousStep = checkHasPreviousStep(orderedSteps, activeStep);
  const hasNextStep = checkHasNextStep(orderedSteps, activeStep);
  const [startInputStatus, setStartInputStatus] = useState<
    'NOT_STARTED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'ROLLED_BACK' | 'ROLLING_BACK'
  >('NOT_STARTED');
  const isRunning = startInputStatus === 'RUNNING' || startInputStatus === 'ROLLING_BACK';
  const hasBeenStarted = startInputStatus !== 'NOT_STARTED';
  const isRollback = startInputStatus === 'ROLLING_BACK' || startInputStatus === 'ROLLED_BACK';

  const {
    createStreamMutation,
    startStreamMutation,
    createPipelineMutation,
    updateRoutingMutation,
    deleteStreamMutation,
    deletePipelineMutation,
    deleteRoutingRuleMutation,
  } = useSetupInputMutations();

  const stepMutations = useMemo<{ [key in ProcessingSteps]?: UseMutationResult }>(
    () => ({
      createStream: createStreamMutation,
      startStream: startStreamMutation,
      createPipeline: createPipelineMutation,
      setupRouting: updateRoutingMutation,
    }),
    [createStreamMutation, startStreamMutation, createPipelineMutation, updateRoutingMutation],
  );

  const rollBackMutations = useMemo<{ [key in ProcessingSteps]?: UseMutationResult }>(
    () => ({
      deleteStream: deleteStreamMutation,
      deletePipeline: deletePipelineMutation,
      deleteRouting: deleteRoutingRuleMutation,
    }),
    [deleteStreamMutation, deletePipelineMutation, deleteRoutingRuleMutation],
  );

  useEffect(() => {
    if (!isRollback) {
      const mutationsArray = Object.entries(stepMutations);

      const hasError = !!mutationsArray.find(([_, mutation]) => mutation.isError);

      const haveAllSucceeded = mutationsArray.every(([_, mutation]) => !mutation.isLoading && mutation.isSuccess);

      if (hasError) {
        setStartInputStatus('FAILED');
      } else if (haveAllSucceeded) {
        setStartInputStatus('SUCCESS');
      }
    }
  }, [stepMutations, isRollback]);

  const createPipeline = async (stream: StreamConfiguration) => {
    const pipeline = {
      title: stream.title,
      description: `Pipeline for Stream: ${stream.title} created by the Input Setup Wizard.`,
    };

    return createPipelineMutation.mutateAsync(pipeline);
  };

  const startInput = async () => {
    const { input } = wizardData;

    if (!input) return;

    InputStatesStore.start(input).finally(() => {
      setStartInputStatus('SUCCESS');
    });
  };

  const stopInput = async () => {
    const { input } = wizardData;

    if (!input) return;

    InputStatesStore.stop(input);
  };

  const setupInput = async () => {
    const routingStepData = getStepData(stepsData, INPUT_WIZARD_STEPS.SETUP_ROUTING) as RoutingStepData;

    sendTelemetry(TELEMETRY_EVENT_TYPE.INPUT_SETUP_WIZARD.START_INPUT, {
      app_pathname: telemetryPathName,
      app_action_value: 'click-input-setup-wizard-start-input',
      chosen_routing_option: routingStepData?.streamType ?? 'UNKNOWN',
    });
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
        updateRoutingMutation
          .mutateAsync({
            input_id: inputId,
            stream_id: routingStepData.streamId,
            remove_from_default: routingStepData.removeMatchesFromDefault,
          })
          .finally(() => {
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

  const rollback = () => {
    const routingStepData = getStepData(stepsData, INPUT_WIZARD_STEPS.SETUP_ROUTING) as RoutingStepData;
    const createdStreamId = createStreamMutation.data?.stream_id;
    const createdPipelineId = createPipelineMutation.data?.id;
    const routingRuleId = updateRoutingMutation.data?.rule_id;

    switch (routingStepData.streamType) {
      case 'NEW':
        stopInput();

        if (routingStepData.shouldCreateNewPipeline && createdPipelineId) {
          deletePipelineMutation.mutateAsync(createdPipelineId);
        }

        if (!createdStreamId) return;

        if (routingRuleId) {
          deleteRoutingRuleMutation.mutateAsync(routingRuleId, {}).finally(() => {
            deleteStreamMutation.mutateAsync(createdStreamId).finally(() => {
              setStartInputStatus('ROLLED_BACK');
            });
          });
        } else {
          deleteStreamMutation.mutateAsync(createdStreamId).finally(() => {
            setStartInputStatus('ROLLED_BACK');
          });
        }

        break;
      case 'EXISTING':
        stopInput();

        if (!routingRuleId) return;

        deleteRoutingRuleMutation.mutateAsync(routingRuleId).finally(() => {
          setStartInputStatus('ROLLED_BACK');
        });

        break;
      case 'DEFAULT':
        stopInput();

        setStartInputStatus('ROLLED_BACK');

        break;

      default:
        break;
    }
  };

  const handleStart = () => {
    setStartInputStatus('RUNNING');
    setupInput();
  };

  const handleRollback = () => {
    setStartInputStatus('ROLLING_BACK');
    rollback();
  };

  const goToInputDiagnosis = () => {
    const { input } = wizardData;

    if (!input) return;

    navigateTo(Routes.SYSTEM.INPUT_DIAGNOSIS(input.id));
  };

  const handleBackClick = () => {
    goToPreviousStep();
  };

  const isInputStartable = () => {
    const routingStepData = getStepData(stepsData, INPUT_WIZARD_STEPS.SETUP_ROUTING) as RoutingStepData;

    if (!routingStepData) return false;
    if (routingStepData.newStream || routingStepData.streamId || routingStepData.streamType === 'DEFAULT') return true;

    return false;
  };

  const getProgressEntityName = (stepName, mutations) => {
    const mutation = mutations[stepName];

    const routingStepData = getStepData(stepsData, INPUT_WIZARD_STEPS.SETUP_ROUTING) as RoutingStepData;

    const name = mutation.data?.title ?? mutation.data?.name ?? undefined;

    switch (stepName) {
      case 'createStream':
      case 'deleteStream':
        return routingStepData?.newStream.title ?? undefined;

      case 'startStream':
        if (routingStepData.streamType === 'NEW') {
          return routingStepData?.newStream.title ?? undefined;
        }

        return name;
      case 'createPipeline':
        return routingStepData?.newStream.title ?? undefined;
      default:
        return name;
    }
  };

  const renderProgressMessages = (mutations: { [key in ProcessingSteps]?: UseMutationResult }) =>
    Object.keys(mutations).map((stepName) => {
      const mutation = mutations[stepName];

      if (!mutation) return null;
      if (mutation.isIdle) return null;

      const name = getProgressEntityName(stepName, mutations);

      return (
        <ProgressMessage
          stepName={stepName as ProcessingSteps}
          isLoading={mutation.isLoading}
          key={stepName}
          isSuccess={mutation.isSuccess}
          name={name}
          isError={mutation.isError}
          errorMessage={mutation.error}
        />
      );
    });

  const renderNextButton = () => {
    if (startInputStatus === 'NOT_STARTED' || startInputStatus === 'ROLLED_BACK') {
      return (
        <Button onClick={handleStart} disabled={!isInputStartable()} bsStyle="primary" data-testid="start-input-button">
          Start Input
        </Button>
      );
    }

    if (startInputStatus === 'FAILED' || startInputStatus === 'ROLLING_BACK') {
      return (
        <Button
          disabled={startInputStatus === 'ROLLING_BACK'}
          onClick={handleRollback}
          bsStyle="primary"
          data-testid="rollback-input-button">
          Rollback Input
        </Button>
      );
    }

    if (hasNextStep) {
      return (
        <Button
          disabled={startInputStatus === 'RUNNING'}
          onClick={goToInputDiagnosis}
          bsStyle="primary"
          data-testid="input-diagnosis-button">
          Input Diagnosis
        </Button>
      );
    }

    return null;
  };

  return (
    <StepWrapper>
      <Row>
        <DescriptionCol md={12}>
          <p>Set up and start the Input according to the configuration made.</p>
        </DescriptionCol>
      </Row>
      <Row>
        <Col md={12}>
          {hasBeenStarted &&
            (isRollback ? (
              <>
                <StyledHeading>Rolling back Input...</StyledHeading>
                {renderProgressMessages(rollBackMutations)}
              </>
            ) : (
              <>
                <StyledHeading>Setting up Input...</StyledHeading>
                {renderProgressMessages(stepMutations)}
                {startInputStatus && (
                  <ProgressMessage
                    stepName="result"
                    isLoading={false}
                    isSuccess={startInputStatus === 'SUCCESS'}
                    isError={startInputStatus === 'FAILED'}
                  />
                )}
              </>
            ))}

          {!hasBeenStarted && !isInputStartable() && (
            <p>Your Input is not ready to be setup yet. Please complete the previous steps.</p>
          )}
        </Col>
      </Row>

      {(hasPreviousStep || hasNextStep) && (
        <Row>
          <ButtonCol md={12}>
            {hasPreviousStep && (
              <Button disabled={isRunning} onClick={handleBackClick}>
                Back
              </Button>
            )}
            {renderNextButton()}
          </ButtonCol>
        </Row>
      )}
    </StepWrapper>
  );
};

export default StartInputStep;
