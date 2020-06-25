import React from 'react';
import PropTypes from 'prop-types';
import naturalSort from 'javascript-natural-sort';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { ButtonToolbar, Button } from 'components/graylog';
import { DataTable } from 'components/common';

import styles from './FieldsList.css';

const HEADERS = ['Field Name', 'Is Key?', 'Value Source', 'Data Type', 'Configuration', 'Actions'];

class FieldsList extends React.Component {
  static propTypes = {
    fields: PropTypes.object.isRequired,
    keys: PropTypes.array.isRequired,
    onAddFieldClick: PropTypes.func.isRequired,
    onEditFieldClick: PropTypes.func.isRequired,
    onRemoveFieldClick: PropTypes.func.isRequired,
  };

  getFieldValueProviderPlugin = (type) => {
    if (type === undefined) {
      return {};
    }

    return PluginStore.exports('fieldValueProviders').find((p) => p.type === type) || {};
  };

  handleAddFieldClick = () => {
    const { onAddFieldClick } = this.props;

    onAddFieldClick();
  };

  handleEditClick = (fieldName) => {
    return () => {
      const { onEditFieldClick } = this.props;

      onEditFieldClick(fieldName);
    };
  };

  handleRemoveClick = (fieldName) => {
    return () => {
      const { onRemoveFieldClick } = this.props;

      onRemoveFieldClick(fieldName);
    };
  };

  providerFormatter = (config) => {
    const configKeys = Object.keys(config).filter((key) => key !== 'type');

    return (
      <p>
        {configKeys.map((key) => {
          return (
            <span key={key} className={styles.providerOptions}>{key}: <em>{JSON.stringify(config[key])}</em></span>
          );
        })}
      </p>
    );
  };

  fieldFormatter = (fieldName) => {
    const { fields, keys } = this.props;
    const config = fields[fieldName];

    const keyIndex = keys.indexOf(fieldName);
    const fieldProviderPlugin = this.getFieldValueProviderPlugin(config.providers[0].type);

    return (
      <tr key={fieldName}>
        <td>{fieldName}</td>
        <td>{keyIndex < 0 ? 'No' : 'Yes'}</td>
        <td>{fieldProviderPlugin.displayName || config.providers[0].type}</td>
        <td>{config.data_type}</td>
        <td>{this.providerFormatter(config.providers[0])}</td>
        <td className={styles.actions}>
          <ButtonToolbar>
            <Button bsStyle="primary" bsSize="xsmall" onClick={this.handleRemoveClick(fieldName)}>
              Remove Field
            </Button>
            <Button bsStyle="info" bsSize="xsmall" onClick={this.handleEditClick(fieldName)}>
              Edit
            </Button>
          </ButtonToolbar>
        </td>
      </tr>
    );
  };

  render() {
    const { fields } = this.props;

    const fieldNames = Object.keys(fields).sort(naturalSort);
    const addCustomFieldButton = (
      <Button bsStyle="success" onClick={this.handleAddFieldClick}>
        Add Custom Field
      </Button>
    );

    if (fieldNames.length === 0) {
      return (
        <>
          <p>
            This Event does not have any custom Fields yet.
          </p>
          {addCustomFieldButton}
        </>
      );
    }

    return (
      <>
        <DataTable id="event-definition-fields"
                   className="table-striped table-hover"
                   headers={HEADERS}
                   rows={fieldNames}
                   dataRowFormatter={this.fieldFormatter}
                   filterKeys={[]} />
        {addCustomFieldButton}
      </>
    );
  }
}

export default FieldsList;
