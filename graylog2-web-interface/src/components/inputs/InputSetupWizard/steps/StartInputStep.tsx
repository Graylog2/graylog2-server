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
import { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';

import { InputStatesStore } from 'stores/inputs/InputStatesStore';
import { Button, Row, Col } from 'components/bootstrap';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';
import type { StepData } from 'components/inputs/InputSetupWizard/types';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import { checkHasPreviousStep, checkHasNextStep, checkIsNextStepDisabled, enableNextStep, updateStepData, getStepData } from 'components/inputs/InputSetupWizard/helpers/stepHelper';
import usePipelineRoutingMutation from 'components/inputs/InputSetupWizard/hooks/usePipelineRoutingMutation';
import type { RoutingStepData } from 'components/inputs/InputSetupWizard/steps/SetupRoutingStep';
import { StreamsActions } from 'stores/streams/StreamsStore';

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

interface StartInputStepData extends StepData {

}

const StartInputStep = () => {
  const currentStepName = INPUT_WIZARD_STEPS.START_INPUT;
  const { goToPreviousStep, goToNextStep, orderedSteps, activeStep, stepsData, setStepsData, wizardData, updateWizardData } = useInputSetupWizard();
  const hasPreviousStep = checkHasPreviousStep(orderedSteps, activeStep);
  const hasNextStep = checkHasNextStep(orderedSteps, activeStep);
  const isNextStepDisabled = checkIsNextStepDisabled(orderedSteps, activeStep, stepsData);
  const [startInputStatus, setStartInputStatus] = useState<'NOT_STARTED' | 'RUNNING' | 'SUCCESS' | 'FAILED'>('NOT_STARTED');
  const isRunningOrDone = startInputStatus === 'RUNNING' || startInputStatus === 'SUCCESS';
  const notStartedOrFailed = startInputStatus === 'NOT_STARTED' || startInputStatus === 'FAILED';
  const hasBeenStarted = startInputStatus !== 'NOT_STARTED';
  const { updateRouting } = usePipelineRoutingMutation();
  const [processMessages, setProcessMessages] = useState<Array<string>>([]);

  useEffect(() => {
    if (orderedSteps && activeStep && stepsData) {
      const withNextStepEnabled = enableNextStep(orderedSteps, activeStep, stepsData);
      setStepsData(withNextStepEnabled);
    } // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const addMessage = (msg: string) => {
    setProcessMessages([...processMessages, msg]);
  };

  const createStream = (stream: any, newPipeline?: boolean = false) => {
    addMessage('Creating stream...');

    // newPipeline

    return StreamsActions.save(stream);
  };

  const startInput = () => {
    const { input } = wizardData;

    if (!input) return;

    addMessage('Starting input...');

    InputStatesStore.start(input)
      .finally(() => {
        setStartInputStatus('SUCCESS');
        addMessage('Input started');
      });
  };

  const setupInput = () => {
    const routingStepData = getStepData(stepsData, INPUT_WIZARD_STEPS.SETUP_ROUTING) as RoutingStepData;
    const { input } = wizardData;
    const inputId = input?.id;

    if (!inputId || !routingStepData) return;

    switch (routingStepData.streamType) {
      case 'NEW':
        createStream(routingStepData.newStream, routingStepData.shouldCreateNewPipeline).then((stream) => {
          addMessage('Stream created');
          addMessage('Setting up routing...');

          updateRouting({ input_id: inputId, stream_id: stream.id }).then(() => {
            addMessage('Routing to new stream set up.');
            startInput();
          });
        });

        break;
      case 'EXISTING':
        addMessage('Setting up routing...');

        updateRouting({ input_id: inputId, stream_id: routingStepData.streamId }).then(() => {
          addMessage('Routing to existing stream set up.');
          startInput();
        });

        break;
      case 'DEFAULT':
        addMessage('Setting up routing...');

        updateRouting({ input_id: inputId, stream_id: routingStepData.defaultStreamId }).then(() => {
          addMessage('Routing to default stream set up.');
          startInput();
        });

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
    setStepsData(
      updateStepData(stepsData, currentStepName, {} as StartInputStepData, true),
    );

    goToPreviousStep();
  };

  return (
    <Row>
      <StepCol md={12}>
        <Row>
          <DescriptionCol md={12}>
            <p>
              Setup your input according to your changes and...better description
            </p>
          </DescriptionCol>
        </Row>
        <Row>
          <Col md={12}>
            {startInputStatus !== 'NOT_STARTED' && (
              <>
                <StyledHeading>Setting up input</StyledHeading>
                {processMessages.length > 0 && (
                  processMessages.map((message) => <p key={message}>{message}</p>,
                  )
                )}
              </>
            )}
            {(notStartedOrFailed) && (
              <Button onClick={handleStart}>Start Input</Button>
            )}
          </Col>
        </Row>

        {(hasPreviousStep || hasNextStep) && (
        <Row>
          <ButtonCol md={12}>
            {(hasPreviousStep) && (<Button disabled={isRunningOrDone} onClick={handleBackClick}>Back</Button>)}
            {hasNextStep && (<Button disabled={isNextStepDisabled || hasBeenStarted} onClick={onNextStep} bsStyle="primary">Input Diagnosis</Button>)}
          </ButtonCol>
        </Row>
        )}
      </StepCol>
    </Row>
  );
};

export default StartInputStep;
