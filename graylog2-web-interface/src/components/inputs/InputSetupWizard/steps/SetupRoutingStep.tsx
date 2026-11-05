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
import { Trans, useTranslation } from 'react-i18next';

import Routes from 'routing/Routes';
import { Link, Select } from 'components/common';
import { Alert, Button, Row, Col, Input } from 'components/bootstrap';
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
  const { t } = useTranslation('inputs');
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
            <StyledAlert bsStyle="warning" title={t('setup_wizard.routing.connected_pipelines_title')}>
              {t('setup_wizard.routing.connected_pipelines')}
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
                <li>{t('setup_wizard.routing.select_destination')}</li>
                <li>
                  <Trans
                    t={t}
                    i18nKey="setup_wizard.routing.recommend_new_stream"
                    components={{ strong: <strong /> }}
                  />
                </li>
                <li>
                  <Trans
                    t={t}
                    i18nKey="setup_wizard.routing.default_stream_fallback"
                    components={{ strong: <strong /> }}
                  />
                </li>
                <li>
                  <Trans t={t} i18nKey="setup_wizard.routing.pipeline_rules_info" components={{ strong: <strong /> }} />
                </li>
              </StyledList>
            </DescriptionCol>
          </Row>

          <Row>
            {!selectedStreamId && (
              <CreateStreamCol md={6}>
                <StyledHeading>{t('setup_wizard.routing.new_stream_heading')}</StyledHeading>
                <RecommendedTooltip opened withArrow position="bottom" label={t('setup_wizard.routing.recommended')}>
                  <Button onClick={handleCreateStream} bsStyle="primary">
                    {t('setup_wizard.routing.create_stream')}
                  </Button>
                </RecommendedTooltip>
              </CreateStreamCol>
            )}
            <ExistingStreamCol md={selectedStreamId ? 12 : 6}>
              <StyledHeading>{t('setup_wizard.routing.existing_stream_heading')}</StyledHeading>
              <Button onClick={handleSelectStream}>{t('setup_wizard.routing.select_stream')}</Button>
            </ExistingStreamCol>
          </Row>
        </>
      )}

      {showNewStreamSection && (
        <Row>
          <Col md={12}>
            <StyledHeading>{t('setup_wizard.routing.create_new_stream')}</StyledHeading>
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
              <StyledLabel htmlFor="streams">{t('setup_wizard.routing.choose_existing_stream')}</StyledLabel>
              <StyledList>
                <li>{t('setup_wizard.routing.existing_stream_selected')}</li>
                <li>
                  <Trans
                    t={t}
                    i18nKey="setup_wizard.routing.pipeline_rules_created"
                    components={{ strong: <strong /> }}
                  />
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
                  aria-label={t('setup_wizard.routing.default_stream')}
                  clearable
                  placeholder={t('setup_wizard.routing.default_stream')}
                  value={selectedStreamId ?? getStepData(stepsData, currentStepName, 'streamId')}
                />
                {!isDefaultStream && (
                  <Input
                    id="existing_remove_matches_from_default_stream"
                    type="checkbox"
                    label={t('setup_wizard.routing.remove_from_default')}
                    title={t('setup_wizard.routing.remove_from_default')}
                    help={<span>{t('setup_wizard.routing.remove_from_default_help')}</span>}
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
            {(hasPreviousStep || showSelectStreamSection) && (
              <Button onClick={handleBackClick}>{t('setup_wizard.back')}</Button>
            )}
            {hasNextStep && (
              <Button disabled={!isStepValid()} onClick={onNextStep} bsStyle="primary">
                {t('common:actions.next')}
              </Button>
            )}
          </ButtonCol>
        </Row>
      )}
    </StepWrapper>
  );
};

export default SetupRoutingStep;
