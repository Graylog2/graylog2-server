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
import React from 'react';
import styled, { css } from 'styled-components';

import { Alert, Row, Col } from 'components/bootstrap';
import useInputReferences from 'components/inputs/InputSetupWizard/hooks/useInputReferences';

import { StyledList } from './StepWrapper';

type Props = {
  inputId?: string;
};

const StreamListWrapper = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
  `,
);

const StyledAlert = styled(Alert)`
  margin-top: 0;
`;

const InputInUseAlert = ({ inputId }: Props = { inputId: undefined }) => {
  const { data: inputReferencesData, isLoading: isLoadingInputReferences } = useInputReferences(inputId);

  if (isLoadingInputReferences) return null;

  if (!inputReferencesData.isInputAlreadyInUse) return null;

  return (
    <Row>
      <Col md={12}>
        <StyledAlert title="Input already in use - Message Duplication Risk!" bsStyle="info">
          {inputReferencesData.stream_refs.length > 0 && (
            <StreamListWrapper>
              This Input is already referenced within the Stream Rules of the following Streams:
              <StyledList>
                {inputReferencesData.stream_refs.map((stream) => (
                  <li key={stream.id}>{stream.name}</li>
                ))}
              </StyledList>
            </StreamListWrapper>
          )}
          {inputReferencesData.pipeline_refs.length > 0 && (
            <>
              This Input is already referenced within the Pipeline Rules of the following Pipelines:
              <StyledList>
                {inputReferencesData.pipeline_refs.map((pipeline) => (
                  <li key={pipeline.id}>{pipeline.name}</li>
                ))}
              </StyledList>
            </>
          )}
        </StyledAlert>
      </Col>
    </Row>
  );
};

export default InputInUseAlert;
