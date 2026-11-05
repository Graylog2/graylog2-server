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
import { useTranslation } from 'react-i18next';

import type FetchError from 'logic/errors/FetchError';
import { Spinner, Icon } from 'components/common';
import type { ProcessingSteps } from 'components/inputs/InputSetupWizard/steps/StartInputStep';

type Props = {
  stepName?: ProcessingSteps;
  isLoading: boolean;
  isSuccess: boolean;
  name?: string;
  isError: boolean;
  errorMessage?: FetchError;
  title?: string;
  details?: string;
};

const SuccessIcon = styled(Icon)(
  ({ theme }) => css`
    color: ${theme.colors.variant.success};
  `,
);

const ErrorIcon = styled(Icon)(
  ({ theme }) => css`
    color: ${theme.colors.variant.danger};
  `,
);

const STEP_KEYS: { [key in ProcessingSteps]: string } = {
  createStream: 'create_stream',
  startStream: 'start_stream',
  createPipeline: 'create_pipeline',
  setupRouting: 'setup_routing',
  result: 'result',
  deleteStream: 'delete_stream',
  deletePipeline: 'delete_pipeline',
  deleteRouting: 'delete_routing',
  connectPipeline: 'connect_pipeline',
};

const NAMED_STEPS: Array<ProcessingSteps> = [
  'createStream',
  'startStream',
  'createPipeline',
  'deleteStream',
  'deletePipeline',
];

const ProgressMessage = ({
  stepName = undefined,
  isLoading,
  isSuccess,
  isError,
  name = undefined,
  errorMessage = undefined,
  title = undefined,
  details = undefined,
}: Props) => {
  const { t } = useTranslation('inputs');

  const text = (status: 'loading' | 'error' | 'success') => {
    if (status === 'loading' && stepName === 'result') return '';

    const key = STEP_KEYS[stepName];

    return name !== undefined && NAMED_STEPS.includes(stepName)
      ? t(`setup_wizard.progress.${status}.${key}_named`, { name })
      : t(`setup_wizard.progress.${status}.${key}`);
  };

  if (isLoading) {
    return (
      <p>
        <Spinner text={title ?? text('loading')} />
      </p>
    );
  }

  if (isError) {
    return (
      <>
        <p>
          <ErrorIcon name="close" title={title ?? text('error')} />
          {title ?? text('error')}
        </p>
        {details ||
          (errorMessage && (
            <p>
              <strong>{t('setup_wizard.progress.details')}</strong> {details ?? errorMessage.message}
            </p>
          ))}
      </>
    );
  }

  if (isSuccess) {
    return (
      <p>
        <SuccessIcon name="check" title={title ?? text('success')} />
        {title ?? text('success')}
      </p>
    );
  }

  return null;
};

export default ProgressMessage;
