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
`;
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
    <Section title="Message Errors" headerLeftSection={<StatusColorIndicator bsStyle={hasError ? 'danger' : 'gray'} />}>
      <StyledP className="description">
        Messages can fail to process at the Input, at the processing pipeline, or when being indexed to the Search
        Cluster. Click on a category to view the associated messages.
      </StyledP>
      <StyledListGroup>
        <StyledListGroupItem>Message Error at Input: {messageErrors.failures_inputs_codecs}</StyledListGroupItem>
        <StyledListGroupItem>
          Message failed to process:{' '}
          <LinkCompoment failureType="processing" inputId={inputId}>
            {messageErrors.failures_processing}
          </LinkCompoment>
        </StyledListGroupItem>
        <StyledListGroupItem>
          Message failed to index:{' '}
          <LinkCompoment failureType="indexing" inputId={inputId}>
            {messageErrors.failures_indexing}
          </LinkCompoment>
        </StyledListGroupItem>
      </StyledListGroup>
    </Section>
  );
};

export default DiagnosisMessageErrors;
