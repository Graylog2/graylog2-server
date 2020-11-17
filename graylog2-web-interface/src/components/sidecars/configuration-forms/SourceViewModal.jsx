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
import lodash from 'lodash';

import { Modal, Button } from 'components/graylog';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import CombinedProvider from 'injection/CombinedProvider';

const { CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');

class SourceViewModal extends React.Component {
  static propTypes = {
    configurationId: PropTypes.string,
    templateString: PropTypes.string,
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
    if (!lodash.isEqual(this.state, SourceViewModal.initialState) && !lodash.isEqual(prevProps, this.props)) {
      this.resetState();
    }
  }

  resetState = () => {
    this.setState(SourceViewModal.initialState);
  };

  open = () => {
    this._loadConfiguration();
    this.sourceModal.open();
  };

  hide = () => {
    this.sourceModal.close();
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
      <BootstrapModalWrapper ref={(c) => { this.sourceModal = c; }}>
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

export default SourceViewModal;
