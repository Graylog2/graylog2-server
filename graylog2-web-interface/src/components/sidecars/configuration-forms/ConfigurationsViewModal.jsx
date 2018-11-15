import PropTypes from 'prop-types';
import React from 'react';
import { Button, Modal } from 'react-bootstrap';
import lodash from 'lodash';

import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import CombinedProvider from 'injection/CombinedProvider';

const { CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');

class ConfigurationsViewModal extends React.Component {
  static propTypes = {
    configurationId: PropTypes.string,
  };

  static defaultProps = {
    configurationId: undefined,
  };

  static initialState = {
    source: undefined,
    name: undefined,
  };

  constructor(props) {
    super(props);
    this.state = ConfigurationsViewModal.initialState;
  }

  componentDidUpdate(prevProps) {
    if (!lodash.isEqual(this.state, ConfigurationsViewModal.initialState) && !lodash.isEqual(prevProps, this.props)) {
      this.resetState();
    }
  }

  resetState = () => {
    this.setState(ConfigurationsViewModal.initialState);
  };

  open = () => {
    this._loadConfiguration();
    this.configurationsModal.open();
  };

  hide = () => {
    this.configurationsModal.close();
  };

  _loadConfiguration = () => {
    // Don't load anything if neither template nor configuration ID are set
    if (!this.props.configurationId) {
      return;
    }

    CollectorConfigurationsActions.getConfiguration(this.props.configurationId)
      .then(
        (configuration) => {
          this.setState({ source: configuration.template, name: configuration.name });
        },
        (error) => {
          this.setState({ source: `Error fetching configuration: ${error.responseMessage || error}` });
        },
      );
  };

  render() {
    return (
      <BootstrapModalWrapper ref={(c) => { this.configurationsModal = c; }}>
        <Modal.Header closeButton>
          <Modal.Title><span>Configuration <em>{this.state.name}</em></span></Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="configuration">
            <pre>
              {this.state.source || '<empty template>'}
            </pre>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button type="button" onClick={this.hide}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  }
}

export default ConfigurationsViewModal;
