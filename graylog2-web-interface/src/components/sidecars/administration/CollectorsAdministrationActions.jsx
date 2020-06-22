import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

import { ButtonToolbar } from 'components/graylog';

import CollectorConfigurationSelector from './CollectorConfigurationSelector';
import CollectorProcessControl from './CollectorProcessControl';

const CollectorsAdministrationActions = createReactClass({
  propTypes: {
    collectors: PropTypes.array.isRequired,
    configurations: PropTypes.array.isRequired,
    selectedSidecarCollectorPairs: PropTypes.array.isRequired,
    onConfigurationSelectionChange: PropTypes.func.isRequired,
    onProcessAction: PropTypes.func.isRequired,
  },

  render() {
    const { collectors, configurations, selectedSidecarCollectorPairs, onConfigurationSelectionChange, onProcessAction } = this.props;
    return (
      <ButtonToolbar>
        <CollectorConfigurationSelector collectors={collectors}
                                        configurations={configurations}
                                        selectedSidecarCollectorPairs={selectedSidecarCollectorPairs}
                                        onConfigurationSelectionChange={onConfigurationSelectionChange} />
        <CollectorProcessControl selectedSidecarCollectorPairs={selectedSidecarCollectorPairs} onProcessAction={onProcessAction} />
      </ButtonToolbar>
    );
  },
});

export default CollectorsAdministrationActions;
