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
import PropTypes from 'prop-types';
import React from 'react';
import isEqual from 'lodash/isEqual';

import { Modal, Button, BootstrapModalWrapper } from 'components/bootstrap';
import { CollectorConfigurationsActions } from 'stores/sidecars/CollectorConfigurationsStore';

class SourceViewModal extends React.Component {
  static propTypes = {
    configurationId: PropTypes.string,
    templateString: PropTypes.string,
    showModal: PropTypes.bool.isRequired,
    onHide: PropTypes.func.isRequired,
  };

  static defaultProps = {
    configurationId: undefined,
    templateString: undefined,
  };

  static initialState = {
    source: undefined,
    name: undefined,
  };

  constructor(props) {
    super(props);
    this.state = SourceViewModal.initialState;
  }

  componentDidUpdate(prevProps) {
    if (!isEqual(this.state, SourceViewModal.initialState) && !isEqual(prevProps, this.props)) {
      this.resetState();
    }

    if (this.props.showModal) {
      this._loadConfiguration();
    }
  }

  resetState = () => {
    this.setState(SourceViewModal.initialState);
  };

  _loadConfiguration = () => {
    // Don't load anything if neither template nor configuration ID are set
    if (!this.props.templateString && !this.props.configurationId) {
      return;
    }

    if (this.props.templateString) {
      CollectorConfigurationsActions.renderPreview(this.props.templateString)
        .then(
          (response) => {
            this.setState({ source: response.preview, name: 'preview' });
          },
          (error) => {
            this.setState({ source: `Error rendering preview: ${error.responseMessage ? error.responseMessage : error}` });
          },
        );
    } else {
      CollectorConfigurationsActions.getConfiguration(this.props.configurationId)
        .then(
          (configuration) => {
            this.setState({ source: configuration.template, name: configuration.name });
          },
          (error) => {
            this.setState({ source: `Error fetching configuration: ${error.responseMessage || error}` });
          },
        );
    }
  };

  render() {
    return (
      <BootstrapModalWrapper showModal={this.props.showModal}
                             onHide={this.props.onHide}
                             data-app-section="collector_configuration_form"
                             data-event-element="Preview Configuration">
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
          <Button type="button" onClick={this.props.onHide}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  }
}

export default SourceViewModal;
