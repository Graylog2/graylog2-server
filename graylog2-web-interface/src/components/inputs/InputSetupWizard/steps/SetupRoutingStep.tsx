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

import { Alert, Button, Row, Col } from 'components/bootstrap';
import { Select } from 'components/common';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';
import { defaultCompare } from 'logic/DefaultCompare';
import type { StepData } from 'components/inputs/InputSetupWizard/types';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import CreateStream from 'components/inputs/InputSetupWizard/steps/components/CreateStream';
import { checkHasPreviousStep, checkHasNextStep, checkIsNextStepDisabled, enableNextStep, updateStepData } from 'components/inputs/InputSetupWizard/helpers/stepHelper';
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

interface RoutingStepData extends StepData {
  streamId: string
}

const SetupRoutingStep = () => {
  const { goToPreviousStep, goToNextStep, orderedSteps, activeStep, stepsData, setStepsData } = useInputSetupWizard();
  const [selectedStreamId, setSelectedStreamId] = useState(undefined);
  const [showCreateStream, setShowCreateStream] = useState<boolean>(false);
  const hasPreviousStep = checkHasPreviousStep(orderedSteps, activeStep);
  const hasNextStep = checkHasNextStep(orderedSteps, activeStep);
  const isNextStepDisabled = checkIsNextStepDisabled(orderedSteps, activeStep, stepsData);
  const currentStepName = INPUT_WIZARD_STEPS.SETUP_ROUTING;
  const { data: streamsData, isInitialLoading: isLoadingStreams } = useStreams({ query: '', page: 1, pageSize: 0, sort: { direction: 'asc', attributeId: 'title' } });
  const streams = streamsData?.list;
  const { data: streamPipelinesData } = usePipelinesConnectedStream(selectedStreamId, !!selectedStreamId);

  useEffect(() => {
    if (orderedSteps && activeStep && stepsData) {
      const withNextStepEnabled = enableNextStep(orderedSteps, activeStep, stepsData);
      setStepsData(withNextStepEnabled);
    } // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
    <Row>
      <StepCol md={12}>
        <Row>
          <DescriptionCol md={12}>
            <p>
              Choose a Destination Stream to Route Messages from this Input to. Messages that are not
              routed to any streams will be sent to the &quot;All Messages&quot; Stream.
            </p>
          </DescriptionCol>
        </Row>
        {selectedStreamId && streamHasConnectedPipelines && (
          <Row>
            <Col md={12}>
              <Alert title="Pipelines connected to stream" bsStyle="info">
                The selected stream has existing pipelines connected to it:
                <ConntectedPipelinesList>
                  {streamPipelinesData.map((pipeline) => <li key={pipeline.title}>{pipeline.title}</li>)}
                </ConntectedPipelinesList>
              </Alert>
            </Col>
          </Row>
        )}
        {showCreateStream ? (<CreateStream />) : (
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
              <Button onClick={() => setShowCreateStream(true)} bsStyle="primary">Create Stream</Button>
            </CreateStreamCol>
          </Row>
        )}
        {(hasPreviousStep || hasNextStep || showCreateStream) && (
        <Row>
          <ButtonCol md={12}>
            {(hasPreviousStep || showCreateStream) && (<Button onClick={handleBackClick}>Back</Button>)}
            {hasNextStep && (<Button disabled={isNextStepDisabled} onClick={onNextStep} bsStyle="primary">Finish & Start Input</Button>)}
          </ButtonCol>
        </Row>
        )}
      </StepCol>
    </Row>
  );
};

export default SetupRoutingStep;
