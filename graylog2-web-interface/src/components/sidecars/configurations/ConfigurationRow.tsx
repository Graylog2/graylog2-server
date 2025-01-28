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
import React from 'react';

import { LinkContainer } from 'components/common/router';
import { Button, ButtonToolbar, MenuItem, DeleteMenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import CollectorIndicator from 'components/sidecars/common/CollectorIndicator';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { MoreActions } from 'components/common/EntityDataTable';

import CopyConfigurationModal from './CopyConfigurationModal';
import styles from './ConfigurationRow.css';

type ConfigurationRowProps = {
  configuration: any;
  collector?: any;
  onCopy: (...args: any[]) => void;
  validateConfiguration: (config: { name: string }) => Promise<{ errors: { name: string[] } }>;
  onDelete: (...args: any[]) => void;
  sendTelemetry?: (...args: any[]) => void;
};

class ConfigurationRow extends React.Component<ConfigurationRowProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    collector: {},
    sendTelemetry: () => {},
  };

  constructor(props) {
    super(props);

    this.state = {
      showModal: false,
    };
  }

  openModal = () => {
    this.setState({ showModal: true });
  };

  closeModal = () => {
    this.setState({ showModal: false });
  };

  _handleDelete = async () => {
    const { configuration, onDelete, sendTelemetry } = this.props;

    sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS.CONFIGURATION_DELETED, {
      app_pathname: 'sidecars',
      app_section: 'configuration',
    });

    // eslint-disable-next-line no-alert
    if (window.confirm(`You are about to delete configuration "${configuration.name}". Are you sure?`)) {
      await onDelete(configuration);
    }
  };

  render() {
    const { collector, configuration, validateConfiguration, onCopy } = this.props;
    const { showModal } = this.state;

    return (
      <tr>
        <td className={styles.name}>{configuration.name}</td>
        <td className={styles.color}><ColorLabel color={configuration.color} size="small" /></td>
        <td>
          <CollectorIndicator collector={collector.name || 'Unknown collector'}
                              operatingSystem={collector.node_operating_system} />
        </td>
        <td className={styles.actions}>
          <ButtonToolbar>
            <LinkContainer to={Routes.SYSTEM.SIDECARS.EDIT_CONFIGURATION(configuration.id)}>
              <Button onClick={this.openModal} bsStyle="info" bsSize="xsmall">Edit</Button>
            </LinkContainer>
            <MoreActions>
              <MenuItem onSelect={() => this.openModal()}>Clone</MenuItem>

              <MenuItem divider />
              <DeleteMenuItem onSelect={this._handleDelete} />
            </MoreActions>
            {showModal && (
            <CopyConfigurationModal configuration={configuration}
                                    onClose={this.closeModal}
                                    showModal={showModal}
                                    validateConfiguration={validateConfiguration}
                                    copyConfiguration={onCopy} />
            )}
          </ButtonToolbar>
        </td>
      </tr>
    );
  }
}

export default withTelemetry(ConfigurationRow);
