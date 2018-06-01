import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Button } from 'react-bootstrap';

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

  renderProcessActionSummary(selectedSidecarCollectorPairs, selectedAction) {
    const actionSummary = (
      <span>
        You are going to <strong>{selectedAction}</strong> log collectors in&nbsp;
        <Pluralize singular="this sidecar" plural="these sidecars" value={selectedSidecarCollectorPairs.length} />:
      </span>
    );
    const formattedSummary = lodash.uniq(selectedSidecarCollectorPairs.map(({ sidecar }) => sidecar.node_name)).join(', ');

    return (
      <BootstrapModalConfirm ref={(c) => { this.modal = c; }}
                             title="Process action summary"
                             onConfirm={this.confirmProcessAction}
                             onCancel={this.cancelProcessAction}>
        <div>
          <p>{actionSummary}</p>
          <p>{formattedSummary}</p>
          <p>Are you sure you want to proceed with this action?</p>
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
