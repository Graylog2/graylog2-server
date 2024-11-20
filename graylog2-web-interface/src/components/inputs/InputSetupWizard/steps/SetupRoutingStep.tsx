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

import { Button, Row, Col } from 'components/bootstrap';
import { Select } from 'components/common';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';
import { StreamsActions } from 'stores/streams/StreamsStore';
import type { Stream } from 'stores/streams/StreamsStore';
import { defaultCompare } from 'logic/DefaultCompare';
import type { StepData } from 'components/inputs/InputSetupWizard/types';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import CreateStream from 'components/inputs/InputSetupWizard/steps/components/CreateStream';
import { checkHasPreviousStep, checkHasNextStep, checkIsNextStepDisabled } from 'components/inputs/InputSetupWizard/helpers/stepHelper';

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
  margin-top: ${theme.spacings.md};
`);

interface RoutingStepData extends StepData {
  stream: string
}

const SetupRoutingStep = () => {
  const { goToPreviousStep, goToNextStep, updateStepData, enableNextStep, orderedSteps, activeStep, stepsData } = useInputSetupWizard();
  const { data: streams, isLoading: isStreamsLoading } = useQuery<Array<Stream>>(['streamsMap'], StreamsActions.listStreams);
  const [selectedStream, setSelectedStream] = useState(undefined);
  const [showCreateStream, setShowCreateStream] = useState<boolean>(false);
  const hasPreviousStep = checkHasPreviousStep(orderedSteps, activeStep);
  const hasNextStep = checkHasNextStep(orderedSteps, activeStep);
  const isNextStepDisabled = checkIsNextStepDisabled(orderedSteps, activeStep, stepsData);
  const currentStepName = INPUT_WIZARD_STEPS.SETUP_ROUTING;

  useEffect(() => {
    enableNextStep();
  }, [enableNextStep]);

  const options = useMemo(() => {
    if (!streams) return [];

    return streams
      .filter(({ is_default, is_editable }) => !is_default && is_editable)
      .sort(({ title: key1 }, { title: key2 }) => defaultCompare(key1, key2))
      .map(({ title, id }) => ({ label: title, value: id }));
  }, [streams]);

  const handleStreamSelect = (stream: string) => {
    setSelectedStream(stream);
  };

  const onNextStep = () => {
    updateStepData(currentStepName, { stream: selectedStream, disabled: false } as RoutingStepData);
    goToNextStep();
  };

  const handleBackClick = () => {
    if (showCreateStream) {
      setShowCreateStream(false);

      return;
    }

    goToPreviousStep();
  };

  return (
    <>
      <Row>
        <DescriptionCol md={12}>
          <p>
            Choose a Destination Stream to Route Messages from this Input to. Messages that are not
            routed to any streams will be sent to the &quot;All Messages&quot; Stream.
          </p>
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
                      value={selectedStream} />
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
