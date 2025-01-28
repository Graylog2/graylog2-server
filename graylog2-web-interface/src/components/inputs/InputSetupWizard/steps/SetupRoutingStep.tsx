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
import { useEffect, useMemo, useState, useCallback } from 'react';
import styled, { css } from 'styled-components';

import { Alert, Button, Row, Col } from 'components/bootstrap';
import { Select } from 'components/common';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';
import useInputSetupWizardSteps from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizardSteps';
import { defaultCompare } from 'logic/DefaultCompare';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import CreateStreamForm from 'components/inputs/InputSetupWizard/steps/components/CreateStreamForm';
import type { StreamFormValues } from 'components/inputs/InputSetupWizard/steps/components/CreateStreamForm';
import { checkHasPreviousStep, checkHasNextStep, checkIsNextStepDisabled, updateStepConfigOrData, getStepConfigOrData } from 'components/inputs/InputSetupWizard/helpers/stepHelper';
import useStreams from 'components/streams/hooks/useStreams';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';

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

const ExistingStreamCol = styled(Col)(({ theme }) => css`
  padding-top: ${theme.spacings.sm};
  padding-bottom: ${theme.spacings.md};
`);

const CreateStreamCol = styled(Col)(({ theme }) => css`
  border-left: 1px solid ${theme.colors.cards.border};
  padding-top: ${theme.spacings.sm};
  padding-bottom: ${theme.spacings.md};
`);

const ButtonCol = styled(Col)(({ theme }) => css`
  display: flex;
  justify-content: flex-end;
  gap: ${theme.spacings.xs};
  margin-top: ${theme.spacings.lg};
`);

const ConntectedPipelinesList = styled.ul`
  list-style-type: disc;
  padding-left: 20px;
`;

export type RoutingStepData = {
  streamId?: string,
  newStream?: StreamFormValues,
  shouldCreateNewPipeline?: boolean,
  streamType: 'NEW' | 'EXISTING' | 'DEFAULT'
}

const SetupRoutingStep = () => {
  const currentStepName = useMemo(() => INPUT_WIZARD_STEPS.SETUP_ROUTING, []);
  const { goToPreviousStep, goToNextStep, orderedSteps, activeStep, stepsConfig } = useInputSetupWizard();
  const { stepsData, setStepsData } = useInputSetupWizardSteps();
  const newStream: StreamFormValues = getStepConfigOrData(stepsData, currentStepName, 'newStream');
  const [selectedStreamId, setSelectedStreamId] = useState(undefined);
  const [showCreateStream, setShowCreateStream] = useState<boolean>(false);
  const hasPreviousStep = checkHasPreviousStep(orderedSteps, activeStep);
  const hasNextStep = checkHasNextStep(orderedSteps, activeStep);
  const isNextStepDisabled = checkIsNextStepDisabled(orderedSteps, activeStep, stepsConfig);
  const { data: streamsData, isInitialLoading: isLoadingStreams } = useStreams({ query: '', page: 1, pageSize: 0, sort: { direction: 'asc', attributeId: 'title' } });
  const streams = streamsData?.list;
  const { data: streamPipelinesData } = usePipelinesConnectedStream(selectedStreamId, !!selectedStreamId);

  const defaultStepData: RoutingStepData = { streamType: 'DEFAULT' };

  const isStepValid = useCallback(() => {
    const stepData = getStepConfigOrData(stepsData, currentStepName);
    if (!stepData) return false;
    const { streamType, streamId } = stepData;

    if (showCreateStream && !newStream) return false;

    switch (streamType) {
      case 'NEW':
        if (!newStream) return false;

        return true;
      case 'EXISTING':
        if (!streamId) return false;

        return true;
      case 'DEFAULT':
        return true;
      default:
        return false;
    }
  }, [currentStepName, newStream, showCreateStream, stepsData]);

  useEffect(() => {
    if (orderedSteps && activeStep && stepsData) {
      if (!getStepConfigOrData(stepsData, currentStepName)?.streamType) {
        const withInitialStepsData = updateStepConfigOrData(stepsData, currentStepName, defaultStepData);

        setStepsData(withInitialStepsData);
      }
    } // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Initial setup: intentionally ommiting dependencies to prevent from unneccesary rerenders

  const options = useMemo(() => {
    if (!streams) return [];

    return streams
      .filter(({ is_default, is_editable }) => !is_default && is_editable)
      .sort(({ title: key1 }, { title: key2 }) => defaultCompare(key1, key2))
      .map(({ title, id }) => ({ label: title, value: id }));
  }, [streams]);

  const handleStreamSelect = (streamId: string) => {
    setSelectedStreamId(streamId);

    if (streamId) {
      setStepsData(
        updateStepConfigOrData(stepsData, currentStepName, { streamId, streamType: 'EXISTING' } as RoutingStepData),
      );
    } else {
      setStepsData(
        updateStepConfigOrData(stepsData, currentStepName, { streamId: undefined, streamType: 'DEFAULT' } as RoutingStepData),
      );
    }
  };

  const handleCreateStream = () => {
    setSelectedStreamId(undefined);

    updateStepConfigOrData(stepsData, currentStepName, defaultStepData);
    setShowCreateStream(true);
  };

  const onNextStep = () => {
    goToNextStep();
  };

  const handleBackClick = () => {
    setStepsData(
      updateStepConfigOrData(stepsData, currentStepName, defaultStepData, true),
    );

    setShowCreateStream(false);

    goToPreviousStep();
  };

  const streamHasConnectedPipelines = streamPipelinesData && streamPipelinesData?.length > 0;

  const submitStreamCreation = ({ create_new_pipeline, ...stream }: StreamFormValues & { create_new_pipeline?: boolean }) => {
    setStepsData(
      updateStepConfigOrData(stepsData, currentStepName, {
        newStream: stream,
        shouldCreateNewPipeline: create_new_pipeline ?? false,
        streamType: 'NEW',
      } as RoutingStepData),
    );
  };

  const backButtonText = newStream ? 'Reset' : 'Back';
  const showNewStreamSection = newStream || showCreateStream;

  return (
    <Row>
      <StepCol md={12}>
        <Row>
          <DescriptionCol md={12}>
            <p>
              Choose a Destination Stream to route Messages from this Input to. Messages that are not
              routed to any Streams will be sent to the &quot;All Messages&quot; Stream.
            </p>
          </DescriptionCol>
        </Row>
        {selectedStreamId && streamHasConnectedPipelines && (
          <Row>
            <Col md={12}>
              <Alert title="Pipelines connected to stream" bsStyle="info">
                The selected Stream has existing Pipelines connected to it:
                <ConntectedPipelinesList>
                  {streamPipelinesData.map((pipeline) => <li key={pipeline.title}>{pipeline.title}</li>)}
                </ConntectedPipelinesList>
              </Alert>
            </Col>
          </Row>
        )}
        {showNewStreamSection ? (
          <Row>
            <Col md={12}>
              <StyledHeading>
                Create new Stream
              </StyledHeading>
              {newStream ? (
                <>
                  <p>This Input will use a new stream: &quot;{newStream.title}&quot;.</p>
                  <p>Matches will {!newStream.remove_matches_from_default_stream && ('not ')}be removed from the Default Stream.</p>
                  {getStepConfigOrData(stepsData, currentStepName, 'shouldCreateNewPipeline') && (<p>A new Pipeline will be created.</p>)}
                </>
              ) : (
                <CreateStreamForm submitForm={submitStreamCreation} />
              )}
            </Col>
          </Row>
        ) : (
          <Row>
            <ExistingStreamCol md={6}>
              <StyledHeading>Choose an existing Stream</StyledHeading>
              {!isLoadingStreams && (
              <Select inputId="streams"
                      onChange={handleStreamSelect}
                      options={options}
                      aria-label="All messages (Default)"
                      clearable
                      placeholder="All messages (Default)"
                      value={selectedStreamId} />
              )}
            </ExistingStreamCol>
            <CreateStreamCol md={6}>
              <StyledHeading>Route to a new Stream</StyledHeading>
              <Button onClick={handleCreateStream} bsStyle="primary">Create Stream</Button>
            </CreateStreamCol>
          </Row>
        )}
        {(hasPreviousStep || hasNextStep || showNewStreamSection) && (
        <Row>
          <ButtonCol md={12}>
            {(hasPreviousStep || showNewStreamSection) && (<Button onClick={handleBackClick}>{backButtonText}</Button>)}
            {hasNextStep && (<Button disabled={isNextStepDisabled || !isStepValid()} onClick={onNextStep} bsStyle="primary">Finish & Start Input</Button>)}
          </ButtonCol>
        </Row>
        )}
      </StepCol>
    </Row>
  );
};

export default SetupRoutingStep;
