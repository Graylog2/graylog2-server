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
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import useHistory from 'routing/useHistory';
import { Icon } from 'components/common';
import useParams from 'routing/useParams';
import { Button, SegmentedControl } from 'components/bootstrap';
import useInputDiagnosis from 'components/inputs/InputDiagnosis/useInputDiagnosis';
import Routes from 'routing/Routes';
import type { Input } from 'components/messageloaders/Types';
import useProductName from 'brand-customization/useProductName';
import InputDiagnosisOverviewTab from 'components/inputs/InputDiagnosis/InputDiagnosisOverviewTab';
import InputDiagnosisRulesTab from 'components/inputs/InputDiagnosis/InputDiagnosisRulesTab';
import { EditInputButton } from 'components/inputs';

const LeftCol = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    place-content: center center;

    > p {
      color: ${theme.colors.gray[50]};
    }
  `,
);

const Header = styled.div(
  ({ theme }) => css`
    display: flex;
    padding-top: ${theme.spacings.sm};
    margin-bottom: ${theme.spacings.md};
    gap: ${theme.spacings.sm};
    margin-left: -15px;
    margin-right: -15px;
    align-items: center;
  `,
);

const Actions = styled.div`
  margin-left: auto;
`;

const ControlsRow = styled.div`
  display: flex;
  align-items: center;
`;

const StyledSegmentedControl = styled(SegmentedControl)(
  ({ theme }) => css`
    background-color: ${theme.colors.section.filled.background};
    border: 1px solid ${theme.colors.section.filled.border};
    margin-bottom: ${theme.spacings.md};

    .mantine-SegmentedControl-innerLabel {
      vertical-align: middle;
    }

    .mantine-SegmentedControl-indicator {
      height: 70% !important;
    }
  `,
);

type DiagnosisTab = 'overview' | 'rules';

const TABS = [
  { value: 'overview' as const, label: 'pages.tab_overview' },
  { value: 'rules' as const, label: 'pages.tab_rules' },
] as const;

const getListeningProtocol = (input?: Input) => {
  if (!input) {
    return undefined;
  }

  const protocolHints = `${input.name ?? ''} ${input.type ?? ''}`.toLowerCase();

  if (protocolHints.includes('udp')) return 'UDP Traffic.';

  return 'TCP Traffic.';
};

const InputDiagnosisPage = () => {
  const { t } = useTranslation('inputs');
  const { inputId } = useParams();
  const { input, inputNodeStates, inputMetrics } = useInputDiagnosis(inputId);
  const { push } = useHistory();
  const productName = useProductName();
  const listeningProtocol = getListeningProtocol(input);
  const [currentTab, setCurrentTab] = useState<DiagnosisTab>('overview');

  const isInputStateDown =
    inputNodeStates.total === 0 ||
    ['FAILED', 'STOPPED', 'FAILING'].some((failedState) => Object.keys(inputNodeStates.states).includes(failedState));
  const hasReceivedMessageMetrics = inputMetrics.incomingMessagesTotal > 0;
  const hasReceivedMessage = inputMetrics.stream_message_count?.some((stream) => stream.count > 0);

  return (
    <>
      <Header>
        <Button onClick={() => push(Routes.SYSTEM.INPUTS)}>
          <Icon name="arrow_left_alt" size="sm" /> {t('pages.back')}
        </Button>
        <LeftCol>
          <h1>{t('pages.diagnosis_title', { name: input?.name })}</h1>

          <p>{t('pages.diagnosis_description')}</p>
        </LeftCol>
      </Header>
      <ControlsRow>
        <StyledSegmentedControl<DiagnosisTab> data={TABS.map(({ value, label }) => ({ value, label: t(label) }))} radius="sm" value={currentTab} onChange={setCurrentTab} />
        {input && (
          <Actions>
            <EditInputButton input={input} />
          </Actions>
        )}
      </ControlsRow>
      {input && currentTab === 'overview' && (
        <InputDiagnosisOverviewTab
          input={input}
          inputId={inputId}
          inputNodeStates={inputNodeStates}
          inputMetrics={inputMetrics}
          productName={productName}
          listeningProtocol={listeningProtocol}
          isInputStateDown={isInputStateDown}
          hasReceivedMessageMetrics={hasReceivedMessageMetrics}
          hasReceivedMessage={hasReceivedMessage}
        />
      )}
      {currentTab === 'rules' && <InputDiagnosisRulesTab inputId={inputId} />}
    </>
  );
};

export default InputDiagnosisPage;
