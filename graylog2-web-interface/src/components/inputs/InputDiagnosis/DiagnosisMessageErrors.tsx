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
import { useTranslation } from 'react-i18next';

import { Section } from 'components/common';
import StatusColorIndicator from 'components/common/StatusColorIndicator';
import { ListGroup, ListGroupItem } from 'components/bootstrap';
import usePluginEntities from 'hooks/usePluginEntities';
import useProductName from 'brand-customization/useProductName';
import HelpPopoverButton from 'components/common/HelpPopoverButton';

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
  display: flex;
`;

const StyledTitle = styled.p(
  ({ theme }) => css`
    font-weight: bold;
    margin-bottom: 0;
    margin-right: 1%;
    width: 30%;

    @media (max-width: ${theme.breakpoints.max.md}) {
      width: 40%;
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
  const { t } = useTranslation('inputs');
  const productName = useProductName();
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
        <HelpPopoverButton
          helpText={`${t('diagnosis.help.message_error_at_input_title')}
${t('diagnosis.help.message_error_at_input', { productName })}

${t('diagnosis.help.message_failed_to_process_title')}
${t('diagnosis.help.message_failed_to_process')}

${t('diagnosis.help.message_failed_to_index_title')}
${t('diagnosis.help.message_failed_to_index')}`}
        />
      }
      title={t('diagnosis.message_errors.title')}>
      <StyledP>{t('diagnosis.message_errors.description')}</StyledP>
      <StyledListGroup>
        <StyledListGroupItem>
          <StyledTitle>
            <LinkCompoment failureType="input" inputId={inputId}>
              {t('diagnosis.message_errors.at_input')}
            </LinkCompoment>
          </StyledTitle>
          {messageErrors.failures_inputs_codecs}
        </StyledListGroupItem>
        <StyledListGroupItem>
          <StyledTitle>
            <LinkCompoment failureType="processing" inputId={inputId}>
              {t('diagnosis.message_errors.failed_to_process')}
            </LinkCompoment>
          </StyledTitle>
          {messageErrors.failures_processing}
        </StyledListGroupItem>
        <StyledListGroupItem>
          <StyledTitle>
            <LinkCompoment failureType="indexing" inputId={inputId}>
              {t('diagnosis.message_errors.failed_to_index')}
            </LinkCompoment>
          </StyledTitle>
          {messageErrors.failures_indexing}
        </StyledListGroupItem>
      </StyledListGroup>
    </Section>
  );
};

export default DiagnosisMessageErrors;
