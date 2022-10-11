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
import React, { useState, useRef } from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Button, Panel, BootstrapModalConfirm } from 'components/bootstrap';
import { Pluralize, SelectPopover } from 'components/common';

const PROCESS_ACTIONS = ['start', 'restart', 'stop'];

const CollectorProcessControl = ({ selectedSidecarCollectorPairs, onProcessAction }) => {
  const [selectedAction, setSelectedAction] = useState(undefined);
  const [isConfigurationWarningHidden, setIsConfigurationWarningHidden] = useState(false);
  const modalRef = useRef(null);

  const resetSelectedAction = () => {
    setSelectedAction(undefined);
  };

  const handleProcessActionSelect = (processAction, hideCallback) => {
    hideCallback();
    setSelectedAction(processAction ? processAction[0] : undefined);
    modalRef.current?.open();
  };

  const confirmProcessAction = (doneCallback) => {
    const callback = () => {
      doneCallback();
      resetSelectedAction();
    };

    onProcessAction(selectedAction, selectedSidecarCollectorPairs, callback);
  };

  const cancelProcessAction = () => {
    resetSelectedAction();
  };

  const hideConfigurationWarning = () => {
    setIsConfigurationWarningHidden(true);
  };

  const renderSummaryContent = (selectedSidecars) => {
    return (
      <>
        <p>
          You are going to <strong>{selectedAction}</strong> log collectors in&nbsp;
          <Pluralize singular="this sidecar" plural="these sidecars" value={selectedSidecars.length} />:
        </p>
        <p>{selectedSidecars.join(', ')}</p>
        <p>Are you sure you want to proceed with this action?</p>
      </>
    );
  };

  const renderConfigurationWarning = () => {
    return (
      <Panel bsStyle="info" header="Collectors without Configuration">
        <p>
          At least one selected Collector is not configured yet. To start a new Collector, assign a
          Configuration to it and the Sidecar will start the process for you.
        </p>
        <p>
          {lodash.capitalize(selectedAction)}ing a Collector without Configuration will have no effect.
        </p>
        <Button bsSize="xsmall" bsStyle="primary" onClick={hideConfigurationWarning}>Understood, continue
          anyway
        </Button>
      </Panel>
    );
  };

  const renderProcessActionSummary = () => {
    const selectedSidecars = lodash.uniq(selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar.node_name));

    // Check if all selected collectors have assigned configurations
    const allHaveConfigurationsAssigned = selectedSidecarCollectorPairs.every(({ collector, sidecar }) => {
      return sidecar.assignments.some(({ collector_id }) => collector_id === collector.id);
    });

    const shouldShowConfigurationWarning = !isConfigurationWarningHidden && !allHaveConfigurationsAssigned;

    return (
      <BootstrapModalConfirm ref={modalRef}
                             title="Process action summary"
                             confirmButtonDisabled={shouldShowConfigurationWarning}
                             onConfirm={confirmProcessAction}
                             onCancel={cancelProcessAction}>
        <div>
          {shouldShowConfigurationWarning
            ? renderConfigurationWarning()
            : renderSummaryContent(selectedSidecars)}
        </div>
      </BootstrapModalConfirm>
    );
  };

  const actionFormatter = (action) => lodash.capitalize(action);

  return (
    <span>
      <SelectPopover id="process-management-action"
                     title="Manage collector processes"
                     triggerNode={(
                       <Button bsStyle="primary" bsSize="small">Process</Button>
                     )}
                     items={PROCESS_ACTIONS}
                     itemFormatter={actionFormatter}
                     selectedItems={selectedAction ? [selectedAction] : []}
                     displayDataFilter={false}
                     onItemSelect={handleProcessActionSelect} />
      {renderProcessActionSummary()}
    </span>
  );
};

CollectorProcessControl.propTypes = {
  selectedSidecarCollectorPairs: PropTypes.array.isRequired,
  onProcessAction: PropTypes.func.isRequired,
};

export default CollectorProcessControl;
