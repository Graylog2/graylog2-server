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

import Routes from 'routing/Routes';
import { Link } from 'components/common/router';
import { Alert, Button, Row, Col, Input } from 'components/bootstrap';
import { Select } from 'components/common';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';
import useInputSetupWizardSteps from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizardSteps';
import useInputSetupWizardStepsHelper from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizardStepsHelper';
import { defaultCompare } from 'logic/DefaultCompare';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import CreateStreamForm from 'components/inputs/InputSetupWizard/steps/components/CreateStreamForm';
import useFilteredStreams from 'components/inputs/InputSetupWizard/hooks/useFilteredStreams';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';
import type { OpenStepsData } from 'components/inputs/InputSetupWizard/types';

import {
  StepWrapper,
  DescriptionCol,
  ButtonCol,
  StyledHeading,
  StyledList,
  RecommendedTooltip,
} from './components/StepWrapper';
import InputInUseAlert from './components/InputInUseAlert';

const ExistingStreamCol = styled(Col)(
  ({ theme }) => css`
    padding-top: ${theme.spacings.sm};
    padding-bottom: ${theme.spacings.md};
  `,
);

const CreateStreamCol = styled(Col)(
  ({ theme }) => css`
    padding-top: ${theme.spacings.sm};
    padding-bottom: ${theme.spacings.md};
    border-right: 1px solid ${theme.colors.cards.border};
  `,
);

const StyledLabel = styled.label(
  ({ theme }) => css`
    font-weight: normal;
    line-height: 1.1;
    margin-bottom: ${theme.spacings.sm};
    display: inline-block;
    font-size: ${theme.fonts.size.h3};
    background: none;
  `,
);

const StyledAlert = styled(Alert)`
  margin-top: 0;
`;

const SetupRoutingStep = () => {
  const [showSelectStream, setShowSelectStream] = useState<boolean>(false);
  const [showCreateStream, setShowCreateStream] = useState<boolean>(false);
  const currentStepName = useMemo(() => INPUT_WIZARD_STEPS.SETUP_ROUTING, []);
  const { goToPreviousStep, goToNextStep, orderedSteps, activeStep, wizardData } = useInputSetupWizard();
  const { stepsData, setStepsData } = useInputSetupWizardSteps<OpenStepsData>();
  const { checkHasPreviousStep, checkHasNextStep, updateStepData, getStepData } =
    useInputSetupWizardStepsHelper<OpenStepsData>();
  const newStream: OpenStepsData['SETUP_ROUTING']['newStream'] = getStepData(stepsData, currentStepName, 'newStream');
  const selectedStreamId = getStepData(stepsData, currentStepName, 'streamId');
  const isDefaultStream = getStepData(stepsData, currentStepName, 'streamType') === 'DEFAULT';
  const removeFromDefault = getStepData(stepsData, currentStepName, 'removeMatchesFromDefault');
  const removeFromDefaultStreamChecked = typeof removeFromDefault === 'undefined' ? true : removeFromDefault;
  const hasPreviousStep = checkHasPreviousStep(orderedSteps, activeStep);
  const hasNextStep = checkHasNextStep(orderedSteps, activeStep);
  const { data: streamsData, isLoading: isLoadingStreams } = useFilteredStreams();
  const streams = streamsData?.streams;
  const { data: streamPipelinesData } = usePipelinesConnectedStream(selectedStreamId, !!selectedStreamId);

  const defaultStepData = { streamType: 'DEFAULT' };

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
  }, [currentStepName, newStream, showCreateStream, stepsData, getStepData]);

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
    if (streamId) {
      setStepsData(
        updateStepData(stepsData, currentStepName, {
          streamId,
          streamType: 'EXISTING',
          removeMatchesFromDefault: removeFromDefaultStreamChecked,
        }),
      );
    } else {
      setStepsData(
        updateStepData(stepsData, currentStepName, {
          streamId: undefined,
          streamType: 'DEFAULT',
          removeMatchesFromDefault: undefined,
        }),
      );
    }
  };

  const handleCreateStream = () => {
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
    setStepsData(updateStepData(stepsData, currentStepName, defaultStepData, true));

    setShowSelectStream(false);
    setShowCreateStream(false);

    goToPreviousStep();
  };

  const handleCheckRemoveFromDefaultStream = () => {
    setStepsData(
      updateStepData(stepsData, currentStepName, {
        removeMatchesFromDefault: !removeFromDefaultStreamChecked,
      }),
    );
  };

  const streamHasConnectedPipelines = streamPipelinesData && streamPipelinesData?.length > 0;

  const submitStreamCreation = ({
    create_new_pipeline,
    ...stream
  }: OpenStepsData['SETUP_ROUTING']['newStream'] & { create_new_pipeline?: boolean }) => {
    setStepsData(
      updateStepData(stepsData, currentStepName, {
        newStream: stream,
        shouldCreateNewPipeline: create_new_pipeline ?? false,
        streamType: 'NEW',
      }),
    );

    onNextStep();
  };

  const showNewStreamSection = newStream || showCreateStream;
  const showSelectStreamSection = selectedStreamId || showSelectStream;

  return (
    <StepWrapper>
      <InputInUseAlert inputId={wizardData?.input?.id} />
      {selectedStreamId && streamHasConnectedPipelines && (
        <Row>
          <Col md={12}>
            <StyledAlert bsStyle="warning" title="Pipelines connected to target Stream">
              We recommending checking the impact of these prior to completing the Input Setup. The target Stream has
              the following Pipelines connected to it:
              <StyledList>
                {streamPipelinesData.map((pipeline) => (
                  <li key={pipeline.title}>
                    <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)} target="_blank">
                      {pipeline.title}
                    </Link>
                  </li>
                ))}
              </StyledList>
            </StyledAlert>
          </Col>
        </Row>
      )}
      {!showNewStreamSection && !showSelectStreamSection && (
        <>
          <Row>
            <DescriptionCol md={12}>
              <StyledList>
                <li>Select a destination Stream to route messages from this input to.</li>
                <li>
                  <strong>We recommend creating a new stream for each new input.</strong> This will help categorise your
                  messages into a basic schema.
                </li>
                <li>
                  Messages that are not routed to any Stream will be routed to the <strong>Default Stream</strong>.
                </li>
                <li>
                  Pipeline rules can be automatically created and attached to the <strong>Default Stream</strong> by
                  this Wizard. These rules will be placed in the system managed Default Routing Pipeline, and will be
                  automatically renamed (or deleted) to accurately reflect the state of this Input.
                </li>
              </StyledList>
            </DescriptionCol>
          </Row>

          <Row>
            {!selectedStreamId && (
              <CreateStreamCol md={6}>
                <StyledHeading>Route to a new Stream</StyledHeading>
                <RecommendedTooltip opened withArrow position="bottom" label="Recommended!">
                  <Button onClick={handleCreateStream} bsStyle="primary">
                    Create Stream
                  </Button>
                </RecommendedTooltip>
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
            <StyledHeading>Create new Stream</StyledHeading>
            <CreateStreamForm
              submitForm={submitStreamCreation}
              handleBackClick={handleBackClick}
              prevCreatedStream={stepsData?.SETUP_ROUTING?.newStream}
              prevShouldCreateNewPipeline={stepsData?.SETUP_ROUTING?.shouldCreateNewPipeline}
            />
          </Col>
        </Row>
      )}
      {showSelectStreamSection && (
        <>
          <Row>
            <DescriptionCol md={12}>
              <StyledLabel htmlFor="streams">Choose an existing Stream</StyledLabel>
              <StyledList>
                <li>Route messages from this input to an existing stream is selected.</li>
                <li>
                  Pipeline Rules will be created when the <strong>Start Input</strong> button is pressed.
                </li>
              </StyledList>
            </DescriptionCol>
          </Row>
          {!isLoadingStreams && (
            <Row>
              <Col md={12}>
                <Select
                  inputId="streams"
                  onChange={handleStreamSelect}
                  options={options}
                  aria-label="Default Stream"
                  clearable
                  placeholder="Default Stream"
                  value={selectedStreamId ?? getStepData(stepsData, currentStepName, 'streamId')}
                />
                {!isDefaultStream && (
                  <Input
                    id="existing_remove_matches_from_default_stream"
                    type="checkbox"
                    label="Remove matches from &lsquo;Default Stream&rsquo;"
                    title="Remove matches from &lsquo;Default Stream&rsquo;"
                    help={
                      <span>
                        Don&apos;t assign messages that match this stream to the &lsquo;Default Stream&rsquo;.
                      </span>
                    }
                    name="existing_remove_matches_from_default_stream"
                    checked={removeFromDefaultStreamChecked}
                    onChange={handleCheckRemoveFromDefaultStream}
                  />
                )}
              </Col>
            </Row>
          )}
        </>
      )}

      {(((hasPreviousStep || hasNextStep) && !showNewStreamSection) || showSelectStreamSection) && (
        <Row>
          <ButtonCol md={12}>
            {(hasPreviousStep || showSelectStreamSection) && <Button onClick={handleBackClick}>Back</Button>}
            {hasNextStep && (
              <Button disabled={!isStepValid()} onClick={onNextStep} bsStyle="primary">
                Next
              </Button>
            )}
          </ButtonCol>
        </Row>
      )}
    </StepWrapper>
  );
};

export default SetupRoutingStep;
