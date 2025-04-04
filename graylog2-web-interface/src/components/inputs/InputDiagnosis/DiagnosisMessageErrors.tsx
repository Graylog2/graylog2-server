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
import React, { useMemo } from 'react';
import styled, { css } from 'styled-components';

import { Section } from 'components/common';
import StatusColorIndicator from 'components/common/StatusColorIndicator';
import { ListGroup, ListGroupItem } from 'components/bootstrap';
import usePluginEntities from 'hooks/usePluginEntities';

import DiagnosisHelp from './DiagnosisHelp';
import { DIAGNOSIS_HELP, TITLE_COLUMN_WIDTH } from './Constants';

type Props = {
  messageErrors: {
    failures_inputs_codecs: number;
    failures_processing: number;
    failures_indexing: number;
  };
  inputId: string;
};

const StyledListGroup = styled(ListGroup)(
  ({ theme }) => css`
    border: 1px solid ${theme.colors.table.row.divider};
    background-color: ${theme.colors.global.contentBackground};
    border-radius: ${theme.spacings.xs};
  `,
);

const StyledListGroupItem = styled(ListGroupItem)`
  background-color: transparent;
  display: flex;
`;

const StyledTitle = styled.p(
  ({ theme }) => css`
    font-weight: bold;
    margin-bottom: 0;
    margin-right: ${theme.spacings.sm};
    width: ${TITLE_COLUMN_WIDTH.wide}px;

    @media (max-width: ${theme.breakpoints.max.xl}) {
      width: ${TITLE_COLUMN_WIDTH.narrow}px;
    }

    @media (max-width: ${theme.breakpoints.max.md}) {
      width: auto;
    }
  `,
);

const StyledP = styled.p(
  ({ theme }) => css`
    &&.description {
      color: ${theme.colors.gray[50]};
    }
  `,
);

const Component = ({ children }) => children;

const DiagnosisMessageErrors = ({ messageErrors, inputId }: Props) => {
  const hasError = Object.keys(messageErrors).some((error) => messageErrors[error] > 0);
  const inputSetupWizards = usePluginEntities('inputSetupWizard');
  const InputFailureLink = useMemo(
    () => inputSetupWizards?.find((plugin) => !!plugin.InputFailureLink)?.InputFailureLink,
    [inputSetupWizards],
  );
  const LinkCompoment = InputFailureLink || Component;

  return (
    <Section
      preHeaderSection={<StatusColorIndicator radius="50%" bsStyle={hasError ? 'danger' : 'gray'} />}
      headerLeftSection={
        <DiagnosisHelp
          helpText={`Message Error at Input:
            ${DIAGNOSIS_HELP.MESSAGE_ERROR_AT_INPUT}

            Message Failed to Process:
            ${DIAGNOSIS_HELP.MESSAGE_FAILED_TO_PROCESS}

            Message Errors:
            ${DIAGNOSIS_HELP.MESSAGE_FAILED_TO_INDEX}
            `}
        />
      }
      title="Message Errors">
      <StyledP>
        Messages can fail to process at the Input, at the processing pipeline, or when being indexed to the Search
        Cluster. Click on a category to view the associated messages.
      </StyledP>
      <StyledListGroup>
        <StyledListGroupItem>
          <LinkCompoment failureType="input" inputId={inputId}>
            <StyledTitle>Message Error at Input:</StyledTitle>
          </LinkCompoment>
          {messageErrors.failures_inputs_codecs}
        </StyledListGroupItem>
        <StyledListGroupItem>
          <LinkCompoment failureType="processing" inputId={inputId}>
            <StyledTitle>Message failed to process:</StyledTitle>
          </LinkCompoment>
          {messageErrors.failures_processing}
        </StyledListGroupItem>
        <StyledListGroupItem>
          <LinkCompoment failureType="indexing" inputId={inputId}>
            <StyledTitle>Message failed to index:</StyledTitle>
          </LinkCompoment>
          {messageErrors.failures_indexing}
        </StyledListGroupItem>
      </StyledListGroup>
    </Section>
  );
};

export default DiagnosisMessageErrors;
