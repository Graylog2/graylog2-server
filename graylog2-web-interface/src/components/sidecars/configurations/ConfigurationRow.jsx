import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import { Button, ButtonToolbar, DropdownButton, MenuItem } from 'components/graylog';
import Routes from 'routing/Routes';
import CollectorIndicator from 'components/sidecars/common/CollectorIndicator';
import ColorLabel from 'components/sidecars/common/ColorLabel';

import CopyConfigurationModal from './CopyConfigurationModal';
import styles from './ConfigurationRow.css';

class ConfigurationRow extends React.Component {
  static propTypes = {
    configuration: PropTypes.object.isRequired,
    collector: PropTypes.object,
    onCopy: PropTypes.func.isRequired,
    validateConfiguration: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
  };

  static defaultProps = {
    collector: {},
  };

  _handleDelete = () => {
    const { configuration, onDelete } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm(`You are about to delete configuration "${configuration.name}". Are you sure?`)) {
      onDelete(configuration);
    }
  };

  render() {
    const { collector, configuration, validateConfiguration, onCopy } = this.props;

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
            <DropdownButton id={`more-actions-${configuration.id}`}
                            title="More actions"
                            bsSize="xsmall"
                            pullRight>
              <CopyConfigurationModal configuration={configuration}
                                      validateConfiguration={validateConfiguration}
                                      copyConfiguration={onCopy} />
              <MenuItem divider />
              <MenuItem onSelect={this._handleDelete}>Delete</MenuItem>
            </DropdownButton>
          </ButtonToolbar>
        </td>
      </tr>
    );
  }
}

export default ConfigurationRow;
