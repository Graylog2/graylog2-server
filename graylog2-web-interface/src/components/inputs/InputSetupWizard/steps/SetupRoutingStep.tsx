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
import { Select, Tooltip } from 'components/common';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';
import useInputSetupWizardSteps from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizardSteps';
import { defaultCompare } from 'logic/DefaultCompare';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import CreateStreamForm from 'components/inputs/InputSetupWizard/steps/components/CreateStreamForm';
import type { StreamFormValues } from 'components/inputs/InputSetupWizard/steps/components/CreateStreamForm';
import { checkHasPreviousStep, checkHasNextStep, updateStepData, getStepData } from 'components/inputs/InputSetupWizard/helpers/stepHelper';
import useStreams from 'components/streams/hooks/useStreams';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';

import { StepWrapper, DescriptionCol, ButtonCol, StyledHeading } from './components/StepWrapper'

const ExistingStreamCol = styled(Col)(({ theme }) => css`
  padding-top: ${theme.spacings.sm};
  padding-bottom: ${theme.spacings.md};
`);

const CreateStreamCol = styled(Col)(({ theme }) => css`
  padding-top: ${theme.spacings.sm};
  padding-bottom: ${theme.spacings.md};
  border-right: 1px solid ${theme.colors.cards.border};
`);

const StyledTooltip = styled(Tooltip)(({ theme }) => css`
  &.mantine-Tooltip-tooltip {
    background-color: ${theme.colors.global.background}!important;
    font-size:  ${theme.fonts.size.small}!important;
  }
`);

const StyledList = styled.ul`
  list-style-type: disc;
  padding-left: 20px;
`;

const StyledLabel = styled.label(({ theme }) => css`
  font-weight: normal;
  line-height: 1.1;
  margin-bottom: ${theme.spacings.sm};
  display: inline-block;
  font-size: ${theme.fonts.size.h3};
  background: none;
`);

export type RoutingStepData = {
  streamId?: string,
  newStream?: StreamFormValues,
  shouldCreateNewPipeline?: boolean,
  streamType: 'NEW' | 'EXISTING' | 'DEFAULT'
}

const SetupRoutingStep = () => {
  const currentStepName = useMemo(() => INPUT_WIZARD_STEPS.SETUP_ROUTING, []);
  const { goToPreviousStep, goToNextStep, orderedSteps, activeStep } = useInputSetupWizard();
  const { stepsData, setStepsData } = useInputSetupWizardSteps();
  const newStream: StreamFormValues = getStepData(stepsData, currentStepName, 'newStream');
  const [selectedStreamId, setSelectedStreamId] = useState(undefined);
  const [showSelectStream, setShowSelectStream] = useState<boolean>(false);
  const [showCreateStream, setShowCreateStream] = useState<boolean>(false);
  const hasPreviousStep = checkHasPreviousStep(orderedSteps, activeStep);
  const hasNextStep = checkHasNextStep(orderedSteps, activeStep);
  const { data: streamsData, isInitialLoading: isLoadingStreams } = useStreams({ query: '', page: 1, pageSize: 0, sort: { direction: 'asc', attributeId: 'title' } });
  const streams = streamsData?.list;
  const { data: streamPipelinesData } = usePipelinesConnectedStream(selectedStreamId, !!selectedStreamId);

  const defaultStepData: RoutingStepData = { streamType: 'DEFAULT' };

  const isStepValid = useCallback(() => {
    const stepData = getStepData(stepsData, currentStepName);
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
      if (!getStepData(stepsData, currentStepName)?.streamType) {
        const withInitialStepsData = updateStepData(stepsData, currentStepName, defaultStepData);

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
        updateStepData(stepsData, currentStepName, { streamId, streamType: 'EXISTING' } as RoutingStepData),
      );
    } else {
      setStepsData(
        updateStepData(stepsData, currentStepName, { streamId: undefined, streamType: 'DEFAULT' } as RoutingStepData),
      );
    }
  };

  const handleCreateStream = () => {
    setSelectedStreamId(undefined);

    updateStepData(stepsData, currentStepName, defaultStepData);
    setShowCreateStream(true);
  };

  const handleSelectStream = () => {
    setShowSelectStream(true);
  };

  const onNextStep = () => {
    goToNextStep();
  };

  const handleBackClick = () => {
    setStepsData(
      updateStepData(stepsData, currentStepName, defaultStepData, true),
    );

    setSelectedStreamId(undefined);
    setShowSelectStream(false);
    setShowCreateStream(false);

    goToPreviousStep();
  };

  const streamHasConnectedPipelines = streamPipelinesData && streamPipelinesData?.length > 0;

  const submitStreamCreation = ({ create_new_pipeline, ...stream }: StreamFormValues & { create_new_pipeline?: boolean }) => {
    setStepsData(
      updateStepData(stepsData, currentStepName, {
        newStream: stream,
        shouldCreateNewPipeline: create_new_pipeline ?? false,
        streamType: 'NEW',
      } as RoutingStepData),
    );
  };

  const backButtonText = newStream ? 'Reset' : 'Back';
  const nextButtonText = showCreateStream || showSelectStream ? 'Finish & Start Input' : 'Skip & Start Input';
  const showNewStreamSection = newStream || showCreateStream;

  return (
    <StepWrapper>
      {!showNewStreamSection && !showSelectStream && (
      <>
        <Row>
          <DescriptionCol md={12}>
            <StyledList>
              <li>
                Select a destination Stream to route messages from this input to.
              </li>
              <li>
                <strong>We recommend creating a new stream for each new input.</strong> This will help categorise your messages into a basic schema.
              </li>
              <li>
                Messages that are not routed to any Stream will be routed to the <strong>Default Stream</strong>.
              </li>
              <li>
                Pipeline rules can be automatically created and attached to the <strong>Default Stream</strong> by this Wizard.
              </li>
            </StyledList>
          </DescriptionCol>
        </Row>

        <Row>
          {!selectedStreamId && (
          <CreateStreamCol md={6}>
            <StyledHeading>Route to a new Stream</StyledHeading>
            <StyledTooltip opened
                            withArrow
                            position="bottom"
                            label="Recommended!">
              <Button onClick={handleCreateStream} bsStyle="primary">Create Stream</Button>
            </StyledTooltip>
          </CreateStreamCol>
          )}
          <ExistingStreamCol md={selectedStreamId ? 12 : 6}>
            <StyledHeading>Route to an existing Stream</StyledHeading>
            <Button onClick={handleSelectStream}>Select Stream</Button>
          </ExistingStreamCol>
        </Row>
      </>
      )}

      {showNewStreamSection && (
      <Row>
        <Col md={12}>
          <StyledHeading>
            Create new Stream
          </StyledHeading>
          {newStream ? (
            <>
              <p>This Input will use a new stream: &quot;{newStream.title}&quot;.</p>
              <p>Matches will {!newStream.remove_matches_from_default_stream && ('not ')}be removed from the Default Stream.</p>
              {getStepData(stepsData, currentStepName, 'shouldCreateNewPipeline') && (<p>A new Pipeline will be created.</p>)}
            </>
          ) : (
            <CreateStreamForm submitForm={submitStreamCreation} />
          )}
        </Col>
      </Row>
      )}
      {showSelectStream && (
        <>
          <Row>
            <DescriptionCol md={12}>
              <StyledLabel>Choose an existing Stream</StyledLabel>
              <StyledList>
                <li>Route messages from this input to an existing stream is selected.</li>
                <li>Pipeline Rules will be created when the <strong>Finish & Start Input</strong> button is pressed.</li>
              </StyledList>
            </DescriptionCol>
          </Row>
          {selectedStreamId && streamHasConnectedPipelines && (
          <Row>
            <Col md={12}>
              <Alert title="Pipelines connected to target Stream"
                      bsStyle="info">
                We recommending checking the impact of these prior to completing the Input Setup. The target Stream has the following Pipelines connected to it:
                <StyledList>
                  {streamPipelinesData.map((pipeline) => <li key={pipeline.title}>{pipeline.title}</li>)}
                </StyledList>
              </Alert>
            </Col>
          </Row>
          )}
            {!isLoadingStreams && (
            <Row>
              <Col md={12}>
                <Select inputId="streams"
                        onChange={handleStreamSelect}
                        options={options}
                        aria-label="All messages (Default)"
                        clearable
                        placeholder="All messages (Default)"
                        value={selectedStreamId} />
              </Col>
            </Row>
            )}
        </>
      )}

      {(hasPreviousStep || hasNextStep || showNewStreamSection) && (
      <Row>
        <ButtonCol md={12}>
          {(hasPreviousStep || showNewStreamSection || showSelectStream) && (<Button onClick={handleBackClick}>{backButtonText}</Button>)}
          {hasNextStep && (<Button disabled={!isStepValid()} onClick={onNextStep} bsStyle="primary">{nextButtonText}</Button>)}
        </ButtonCol>
      </Row>
      )}
    </StepWrapper>
  );
};

export default SetupRoutingStep;
