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
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import { Button, Alert, Row, Col, Table } from 'components/bootstrap';
import { Link } from 'components/common';
import Routes from 'routing/Routes';
import InputStateBadge from 'components/inputs/InputStateBadge';
import usePermissions from 'hooks/usePermissions';
import useInputsStates from 'hooks/useInputsStates';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import { useCollectorInputDetails, useCollectorInputMutations } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import { classifyInputBind } from '../hooks/telemetry-helpers';

const SectionTitle = styled.h3(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
  `,
);

type Props = {
  isInitialSetup: boolean;
};

const IngestEndpointStatus = ({ isInitialSetup }: Props) => {
  const { t } = useTranslation('collectors');
  const { isPermitted } = usePermissions();
  const { createCollectorInput } = useCollectorInputMutations();
  const { collectorInputIds, loadedInputs, unreadableCount, isLoading } = useCollectorInputDetails();
  const { data: inputStates, isLoading: isLoadingInputStates } = useInputsStates({
    enabled: collectorInputIds.length > 0,
  });
  const sendTelemetry = useSendCollectorsTelemetry();

  const canCreateInputs = isPermitted([
    'inputs:create',
    'input_types:create:org.graylog.collectors.input.CollectorIngestHttpInput',
  ]);

  const hasInputs = collectorInputIds.length > 0;

  const hasRunningInput = loadedInputs.some((input) => {
    const nodeStates = inputStates?.[input.id];
    if (!nodeStates) return false;

    return Object.values(nodeStates).some((entry) => entry.state === 'RUNNING');
  });

  const handleCreateInput = () => createCollectorInput();

  if (isLoading) {
    return null;
  }

  if (isInitialSetup && !hasInputs) {
    return null;
  }

  return (
    <Row className="content">
      <Col md={12}>
        <SectionTitle>{t('ingest_inputs.title')}</SectionTitle>
        {!isInitialSetup && !hasInputs && (
          <Alert bsStyle="info">
            {t('ingest_inputs.none')}
            {canCreateInputs && (
              <>
                {' '}
                <Button bsSize="xsmall" onClick={handleCreateInput}>
                  {t('ingest_endpoint.create_input')}
                </Button>
              </>
            )}
          </Alert>
        )}
        {loadedInputs.length === 0 && unreadableCount > 0 && (
          <p>{t('ingest_inputs.unreadable', { count: unreadableCount })}</p>
        )}
        {!isLoadingInputStates && !hasRunningInput && loadedInputs.length > 0 && (
          <Alert bsStyle="info">{t('ingest_inputs.none_running')}</Alert>
        )}
        {loadedInputs.length > 0 && (
          <Table condensed>
            <thead>
              <tr>
                <th>{t('ingest_inputs.name')}</th>
                <th>{t('ingest_inputs.status')}</th>
                <th>{t('ingest_inputs.bind_address')}</th>
                <th>{t('ingest_inputs.port')}</th>
                <th aria-label={t('enrolling_hosts.actions')} />
              </tr>
            </thead>
            <tbody>
              {loadedInputs.map((input) => (
                <tr key={input.id}>
                  <td>{input.title}</td>
                  <td>
                    <InputStateBadge input={input} inputStates={inputStates} />
                  </td>
                  <td>{String(input.attributes?.bind_address ?? '')}</td>
                  <td>{String(input.attributes?.port ?? '')}</td>
                  <td>
                    <Link
                      to={`${Routes.SYSTEM.INPUTS}?query=id%3A${input.id}`}
                      onClick={() => {
                        const nodeStates = inputStates?.[input.id];
                        const isRunning = nodeStates
                          ? Object.values(nodeStates).some((entry) => entry.state === 'RUNNING')
                          : false;

                        sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.SETTINGS.DIAGNOSTICS_OPENED, {
                          app_action_value: 'manage-input-link',
                          input_id: input.id,
                          input_bind_type: classifyInputBind(String(input.attributes?.bind_address ?? '')),
                          input_port: input.attributes?.port,
                          input_running: isRunning,
                        });
                      }}>
                      {t('ingest_inputs.manage')}
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
        {unreadableCount > 0 && loadedInputs.length > 0 && (
          <p>{t('ingest_inputs.additional_unreadable', { count: unreadableCount })}</p>
        )}
      </Col>
    </Row>
  );
};

export default IngestEndpointStatus;
