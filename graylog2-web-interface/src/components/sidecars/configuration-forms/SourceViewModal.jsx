import PropTypes from 'prop-types';
import React from 'react';

import { Button, Modal } from 'react-bootstrap';

import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import CombinedProvider from 'injection/CombinedProvider';

const { CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');

const SourceViewModal = React.createClass({
  propTypes: {
    configurationId: PropTypes.string,
    templateString: PropTypes.string,
  },

  getInitialState() {
    return {
      source: undefined,
      name: undefined,
    };
  },

  open() {
    this._loadConfiguration();
    this.sourceModal.open();
  },

  hide() {
    this.sourceModal.close();
  },

  _loadConfiguration() {
    if (this.props.templateString) {
      CollectorConfigurationsActions.renderPreview(this.props.templateString)
        .then((response) => {
          this.setState({ source: response.preview, name: 'preview' });
        });
    } else {
      CollectorConfigurationsActions.getConfiguration(this.props.configurationId)
        .then((configuration) => {
          this.setState({ source: configuration.template, name: configuration.name });
        });
    }
  },

  render() {
    return (
      <BootstrapModalWrapper ref={(c) => { this.sourceModal = c; }}>
        <Modal.Header closeButton>
          <Modal.Title><span>Configuration <em>{this.state.name}</em></span></Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="configuration">
            <pre>
              {this.state.source}
            </pre>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button type="button" onClick={this.hide}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  },
});

export default SourceViewModal;
