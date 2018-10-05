import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Button, Panel } from 'react-bootstrap';

import { Pluralize, SelectPopover } from 'components/common';
import { BootstrapModalConfirm } from 'components/bootstrap';

const PROCESS_ACTIONS = ['start', 'restart', 'stop'];

const CollectorProcessControl = createReactClass({
  propTypes: {
    selectedSidecarCollectorPairs: PropTypes.array.isRequired,
    onProcessAction: PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      selectedAction: undefined,
      isConfigurationWarningHidden: false,
    };
  },

  resetSelectedAction() {
    this.setState({ selectedAction: undefined });
  },

  handleProcessActionSelect(processAction, hideCallback) {
    hideCallback();
    this.setState({ selectedAction: processAction ? processAction[0] : undefined }, this.modal.open);
  },

  confirmProcessAction(doneCallback) {
    const callback = () => {
      doneCallback();
      this.resetSelectedAction();
    };
    this.props.onProcessAction(this.state.selectedAction, this.props.selectedSidecarCollectorPairs, callback);
  },

  cancelProcessAction() {
    this.resetSelectedAction();
  },

  hideConfigurationWarning() {
    this.setState({ isConfigurationWarningHidden: true });
  },


  renderSummaryContent(selectedAction, selectedSidecars) {
    return (
      <React.Fragment>
        <p>
          You are going to <strong>{selectedAction}</strong> log collectors in&nbsp;
          <Pluralize singular="this sidecar" plural="these sidecars" value={selectedSidecars.length} />:
        </p>
        <p>{selectedSidecars.join(', ')}</p>
        <p>Are you sure you want to proceed with this action?</p>
      </React.Fragment>
    );
  },

  renderConfigurationWarning(selectedAction) {
    return (
      <Panel bsStyle="info" header="Collectors without Configuration">
        <p>
          At least one selected Collector is not configured yet. To start a new Collector, assign a
          Configuration to it and the Sidecar will start the process for you.
        </p>
        <p>
          {lodash.capitalize(selectedAction)}ing a Collector without Configuration will have no effect.
        </p>
        <Button bsSize="xsmall" bsStyle="primary" onClick={this.hideConfigurationWarning}>Understood, continue
          anyway</Button>
      </Panel>
    );
  },

  renderProcessActionSummary(selectedSidecarCollectorPairs, selectedAction) {
    const { isConfigurationWarningHidden } = this.state;
    const selectedSidecars = lodash.uniq(selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar.node_name));

    // Check if all selected collectors have assigned configurations
    const allHaveConfigurationsAssigned = selectedSidecarCollectorPairs.every(({ collector, sidecar }) => {
      // eslint-disable-next-line camelcase
      return sidecar.assignments.some(({ collector_id }) => collector_id === collector.id);
    });

    const shouldShowConfigurationWarning = !isConfigurationWarningHidden && !allHaveConfigurationsAssigned;

    return (
      <BootstrapModalConfirm ref={(c) => { this.modal = c; }}
                             title="Process action summary"
                             confirmButtonDisabled={shouldShowConfigurationWarning}
                             onConfirm={this.confirmProcessAction}
                             onCancel={this.cancelProcessAction}>
        <div>
          {shouldShowConfigurationWarning ?
            this.renderConfigurationWarning(selectedAction) :
            this.renderSummaryContent(selectedAction, selectedSidecars)}
        </div>
      </BootstrapModalConfirm>
    );
  },

  render() {
    const { selectedSidecarCollectorPairs } = this.props;
    const { selectedAction } = this.state;

    const actionFormatter = action => lodash.capitalize(action);

    return (
      <span>
        <SelectPopover id="process-management-action"
                       title="Manage collector processes"
                       triggerNode={<Button bsSize="small"
                                            bsStyle="link">Process <span className="caret" /></Button>}
                       items={PROCESS_ACTIONS}
                       itemFormatter={actionFormatter}
                       selectedItems={selectedAction ? [selectedAction] : []}
                       displayDataFilter={false}
                       onItemSelect={this.handleProcessActionSelect} />
        {this.renderProcessActionSummary(selectedSidecarCollectorPairs, selectedAction)}
      </span>
    );
  },
});

export default CollectorProcessControl;
