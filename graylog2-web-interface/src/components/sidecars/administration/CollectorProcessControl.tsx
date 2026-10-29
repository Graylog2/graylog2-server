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
import { Trans, useTranslation } from 'react-i18next';
import uniq from 'lodash/uniq';

import { Button, Panel, BootstrapModalConfirm } from 'components/bootstrap';
import { SelectPopover } from 'components/common';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { SidecarCollectorPairType } from '../types';

const PROCESS_ACTIONS = ['start', 'restart', 'stop'];

type Props = {
  selectedSidecarCollectorPairs: SidecarCollectorPairType[];
  onProcessAction: (action: string, pairs: SidecarCollectorPairType[], callback: () => void) => void;
};

const CollectorProcessControl = ({ selectedSidecarCollectorPairs, onProcessAction }: Props) => {
  const { t } = useTranslation('sidecars');
  const [selectedAction, setSelectedAction] = useState<string>('');
  const [isConfigurationWarningHidden, setIsConfigurationWarningHidden] = useState(false);
  const [showModal, setShowModal] = useState<boolean>(false);
  const sendTelemetry = useSendTelemetry('administration');

  const resetSelectedAction = () => {
    setSelectedAction(undefined);
  };

  const handleProcessActionSelect = (processAction: string[], hideCallback: () => void) => {
    hideCallback();
    setSelectedAction(processAction ? processAction[0] : undefined);
    setShowModal(true);
  };

  const cancelProcessAction = () => {
    resetSelectedAction();
    setShowModal(false);
  };

  const confirmProcessAction = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS.PROCESS_ACTION_SET, {
      app_pathname: 'sidecars',
      event_details: {
        action: selectedAction,
      },
    });

    onProcessAction(selectedAction, selectedSidecarCollectorPairs, cancelProcessAction);
  };

  const hideConfigurationWarning = () => {
    setIsConfigurationWarningHidden(true);
  };

  const renderSummaryContent = (selectedSidecars: string[]) => (
    <>
      <p>
        <Trans
          t={t}
          i18nKey={`administration.process.summary.${selectedAction}`}
          count={selectedSidecars.length}
          components={{ action: <strong /> }}
        />
      </p>
      <p>{selectedSidecars.join(', ')}</p>
      <p>{t('administration.process.confirm')}</p>
    </>
  );

  const renderConfigurationWarning = () => (
    <Panel bsStyle="info" header={t('administration.process.warning_title')}>
      <p>{t('administration.process.warning_text')}</p>
      <p>{t(`administration.process.no_effect.${selectedAction}`)}</p>
      <Button bsSize="xsmall" bsStyle="primary" onClick={hideConfigurationWarning}>
        {t('administration.process.understood')}
      </Button>
    </Panel>
  );

  const renderProcessActionSummary = () => {
    const selectedSidecars = uniq<string>(selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar.node_name));

    // Check if all selected collectors have assigned configurations
    const allHaveConfigurationsAssigned = selectedSidecarCollectorPairs.every(({ collector, sidecar }) =>
      sidecar.assignments.some(({ collector_id }) => collector_id === collector.id),
    );

    const shouldShowConfigurationWarning = !isConfigurationWarningHidden && !allHaveConfigurationsAssigned;

    return (
      <BootstrapModalConfirm
        showModal={showModal}
        title={t('administration.process.summary_title')}
        confirmButtonDisabled={shouldShowConfigurationWarning}
        onConfirm={confirmProcessAction}
        onCancel={cancelProcessAction}>
        <div>
          {shouldShowConfigurationWarning ? renderConfigurationWarning() : renderSummaryContent(selectedSidecars)}
        </div>
      </BootstrapModalConfirm>
    );
  };

  const actionFormatter = (action: string) => t(`administration.process.actions.${action}`);

  return (
    <span>
      <SelectPopover
        title={t('administration.process.title')}
        triggerNode={
          <Button bsStyle="primary" bsSize="small">
            {t('administration.process.button')}
          </Button>
        }
        items={PROCESS_ACTIONS}
        itemFormatter={actionFormatter}
        selectedItems={selectedAction ? [selectedAction] : []}
        displayDataFilter={false}
        onItemSelect={handleProcessActionSelect}
      />
      {renderProcessActionSummary()}
    </span>
  );
};

export default CollectorProcessControl;
