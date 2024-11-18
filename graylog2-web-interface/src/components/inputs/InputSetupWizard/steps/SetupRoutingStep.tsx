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
import { useMemo, useState } from 'react';
import styled, { css } from 'styled-components';
import { useQuery } from '@tanstack/react-query';

import { Button, Row, Col } from 'components/bootstrap';
import { Select } from 'components/common';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';
import { StreamsActions } from 'stores/streams/StreamsStore';
import type { Stream } from 'stores/streams/StreamsStore';
import { defaultCompare } from 'logic/DefaultCompare';
import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';

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

const SetupRoutingStep = () => {
  const { hasPreviousStep, hasNextStep, setStepData } = useInputSetupWizard();
  const { data: streams, isLoading: isStreamsLoading } = useQuery<Array<Stream>>(['streamsMap'], StreamsActions.listStreams);
  const [selectedStream, setSelectedStream] = useState(undefined);

  const options = useMemo(() => {
    if (!streams) return [];

    return streams
      .filter(({ is_default, is_editable }) => !is_default && is_editable)
      .sort(({ title: key1 }, { title: key2 }) => defaultCompare(key1, key2))
      .map(({ title, id }) => ({ label: title, value: id }));
  }, [streams]);

  const onNextStep = (createNewStream = false) => {
    setStepData(INPUT_WIZARD_STEPS.SETUP_ROUTING, { stream: selectedStream, createNewStream });
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
      <Row>
        <Col md={6}>
          <StyledHeading>Choose an existing Stream</StyledHeading>
          {!isStreamsLoading && (
          <Select inputId="streams"
                  onChange={setSelectedStream}
                  options={options}
                  clearable
                  placeholder="All messages (Default)"
                  value={selectedStream} />
          )}
        </Col>
        <Col md={6}>
          <StyledHeading>Route to a new Stream</StyledHeading>

          <Button onClick={() => onNextStep(true)} bsStyle="primary">Create Stream</Button>
        </Col>
      </Row>
      <Row>
        {(hasPreviousStep || hasNextStep) && (
          <ButtonCol md={12}>
            {hasPreviousStep && (<Button>Back</Button>)}
            {hasNextStep && (<Button onClick={onNextStep} bsStyle="primary">Finish & Start Input</Button>)}
          </ButtonCol>
        )}
      </Row>
    </>
  );
};

export default SetupRoutingStep;
