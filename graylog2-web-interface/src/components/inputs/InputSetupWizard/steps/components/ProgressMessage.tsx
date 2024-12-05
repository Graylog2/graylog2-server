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

import type FetchError from 'logic/errors/FetchError';
import { Spinner } from 'components/common';
import type { ProcessingSteps } from 'components/inputs/InputSetupWizard/steps/StartInputStep';

type Props = {
  stepName: ProcessingSteps,
  isLoading: boolean,
  isSuccess: boolean,
  isError: boolean,
  errorMessage?: FetchError,
}

// const NewIndexSetButton = styled(Button)(({ theme }) => css`
//   margin-bottom: ${theme.spacings.xs};
// `);

// const SubmitCol = styled(Col)`
//   display: flex;
//   justify-content: flex-end;
// `;
// todo typing
const ProgressMessage = ({ stepName, isLoading, isSuccess, isError, errorMessage = undefined } : Props) => {
  const loadingText: {[key in ProcessingSteps]: string} = {
    createStream: 'Creating stream...',
    startStream: 'Starting stream...',
    createPipeline: 'Creating pipeline...',
    setupRouting: 'Setting up routing...',
    startInput: 'Starting input...',
  };

  const errorText: {[key in ProcessingSteps]: string} = {
    createStream: 'Creating stream failed',
    startStream: 'Starting stream failed',
    createPipeline: 'Creating pipeline failed',
    setupRouting: 'Setting up routing failed',
    startInput: "Input couldn't be started",
  };

  const successText: {[key in ProcessingSteps]: string} = {
    createStream: 'Stream created',
    startStream: 'Stream started',
    createPipeline: 'Pipeline created',
    setupRouting: 'Routing set up',
    startInput: 'Input started',
  };

  if (isLoading) {
    return <p><Spinner text={loadingText[stepName]} /></p>;
  }

  if (isError) {
    return (
      <>
        <p>{errorText[stepName]}</p>
        {errorMessage && (<p><strong>Details:</strong> {errorMessage.message}</p>)}
      </>
    );
  }

  if (isSuccess) {
    return (
      <p>{successText[stepName]}</p>
    );
  }

  return null;
};

export default ProgressMessage;
