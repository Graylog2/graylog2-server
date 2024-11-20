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
import { useEffect, useMemo, useState } from 'react';
import styled, { css } from 'styled-components';
import { useQuery } from '@tanstack/react-query';

import { Alert, Button, Row, Col } from 'components/bootstrap';
import { Select } from 'components/common';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';
import { StreamsActions } from 'stores/streams/StreamsStore';
import type { Stream } from 'stores/streams/StreamsStore';
import { defaultCompare } from 'logic/DefaultCompare';
import type { StepData } from 'components/inputs/InputSetupWizard/types';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import CreateStream from 'components/inputs/InputSetupWizard/steps/components/CreateStream';
import { checkHasPreviousStep, checkHasNextStep, checkIsNextStepDisabled, enableNextStep, updateStepData } from 'components/inputs/InputSetupWizard/helpers/stepHelper';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';

const DescriptionCol = styled(Col)(({ theme }) => css`
  margin-bottom: ${theme.spacings.sm};
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

const ConntectedPipelinesList = styled.ul`
  list-style-type: disc;
  padding-left: 20px;
`;

interface RoutingStepData extends StepData {
  streamId: string
}

const SetupRoutingStep = () => {
  const { goToPreviousStep, goToNextStep, orderedSteps, activeStep, stepsData, setStepsData } = useInputSetupWizard();
  const { data: streams, isLoading: isStreamsLoading } = useQuery<Array<Stream>>(['streamsMap'], StreamsActions.listStreams);
  const [selectedStreamId, setSelectedStreamId] = useState(undefined);
  const [showCreateStream, setShowCreateStream] = useState<boolean>(false);
  const hasPreviousStep = checkHasPreviousStep(orderedSteps, activeStep);
  const hasNextStep = checkHasNextStep(orderedSteps, activeStep);
  const isNextStepDisabled = checkIsNextStepDisabled(orderedSteps, activeStep, stepsData);
  const currentStepName = INPUT_WIZARD_STEPS.SETUP_ROUTING;

  useEffect(() => {
    if (orderedSteps && activeStep && stepsData) {
      const withNextStepEnabled = enableNextStep(orderedSteps, activeStep, stepsData);
      setStepsData(withNextStepEnabled);
    } // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const { data: streamPipelinesData } = usePipelinesConnectedStream(selectedStreamId, !!selectedStreamId);

  const options = useMemo(() => {
    if (!streams) return [];

    return streams
      .filter(({ is_default, is_editable }) => !is_default && is_editable)
      .sort(({ title: key1 }, { title: key2 }) => defaultCompare(key1, key2))
      .map(({ title, id }) => ({ label: title, value: id }));
  }, [streams]);

  const handleStreamSelect = (streamId: string) => {
    setSelectedStreamId(streamId);
  };

  const onNextStep = () => {
    setStepsData(
      updateStepData(stepsData, currentStepName, { streamId: selectedStreamId } as RoutingStepData),
    );

    goToNextStep();
  };

  const handleBackClick = () => {
    if (showCreateStream) {
      setShowCreateStream(false);

      return;
    }

    goToPreviousStep();
  };

  const streamHasConnectedPipelines = streamPipelinesData && streamPipelinesData?.length > 0;

  return (
    <>
      <Row>
        <DescriptionCol md={12}>
          <p>
            Choose a Destination Stream to Route Messages from this Input to. Messages that are not
            routed to any streams will be sent to the &quot;All Messages&quot; Stream.
          </p>
          {selectedStreamId && streamHasConnectedPipelines && (
          <Alert title="Pipelines connected to stream" bsStyle="info">
            The selected stream has existing pipelines connected to it:
            <ConntectedPipelinesList>
              {streamPipelinesData.map((pipeline) => <li key={pipeline.title}>{pipeline.title}</li>)}
            </ConntectedPipelinesList>
          </Alert>
          )}
        </DescriptionCol>
      </Row>
      {showCreateStream ? (<CreateStream />) : (
        <Row>
          <Col md={6}>
            <StyledHeading>Choose an existing Stream</StyledHeading>
            {!isStreamsLoading && (
              <Select inputId="streams"
                      onChange={handleStreamSelect}
                      options={options}
                      clearable
                      placeholder="All messages (Default)"
                      value={selectedStreamId} />
            )}
          </Col>
          <Col md={6}>
            <StyledHeading>Route to a new Stream</StyledHeading>
            <Button onClick={() => setShowCreateStream(true)} bsStyle="primary">Create Stream</Button>
          </Col>
        </Row>
      )}
      <Row>
        {(hasPreviousStep || hasNextStep || showCreateStream) && (
          <ButtonCol md={12}>
            {(hasPreviousStep || showCreateStream) && (<Button onClick={handleBackClick}>Back</Button>)}
            {hasNextStep && (<Button disabled={isNextStepDisabled} onClick={onNextStep} bsStyle="primary">Finish & Start Input</Button>)}
          </ButtonCol>
        )}
      </Row>
    </>
  );
};

export default SetupRoutingStep;
