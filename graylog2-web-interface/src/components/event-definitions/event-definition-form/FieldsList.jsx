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
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { ButtonToolbar, Button } from 'components/bootstrap';
import { DataTable } from 'components/common';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import withLocation from 'routing/withLocation';

import styles from './FieldsList.css';

const HEADERS = ['Field Name', 'Is Key?', 'Value Source', 'Data Type', 'Configuration', 'Actions'];

const getFieldValueProviderPlugin = (type) => {
  if (type === undefined) {
    return {};
  }

  return PluginStore.exports('fieldValueProviders').find((p) => p.type === type) || {};
};

const providerFormatter = (config) => {
  const configKeys = Object.keys(config).filter((key) => key !== 'type');

  return (
    <p>
      {configKeys.map((key) => (
        <span key={key} className={styles.providerOptions}>{key}: <em>{JSON.stringify(config[key])}</em></span>
      ))}
    </p>
  );
};

class FieldsList extends React.Component {
  static propTypes = {
    fields: PropTypes.object.isRequired,
    keys: PropTypes.array.isRequired,
    onAddFieldClick: PropTypes.func.isRequired,
    onEditFieldClick: PropTypes.func.isRequired,
    onRemoveFieldClick: PropTypes.func.isRequired,
    sendTelemetry: PropTypes.func.isRequired,
    location: PropTypes.object.isRequired,
  };

  handleAddFieldClick = () => {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_FIELDS.ADD_CUSTOM_FIELD_CLICKED, {
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_section: 'event-definition-fields',
      app_action_value: 'add-custom-field-button',
    });

    const { onAddFieldClick } = this.props;

    onAddFieldClick();
  };

  handleEditClick = (fieldName) => () => {
    const { onEditFieldClick } = this.props;

    onEditFieldClick(fieldName);
  };

  handleRemoveClick = (fieldName) => () => {
    const { onRemoveFieldClick } = this.props;

    onRemoveFieldClick(fieldName);
  };

  fieldFormatter = (fieldName) => {
    const { fields, keys } = this.props;
    const config = fields[fieldName];

    const keyIndex = keys.indexOf(fieldName);
    const fieldProviderPlugin = getFieldValueProviderPlugin(config.providers[0].type);

    return (
      <tr key={fieldName}>
        <td>{fieldName}</td>
        <td>{keyIndex < 0 ? 'No' : 'Yes'}</td>
        <td>{fieldProviderPlugin.displayName || config.providers[0].type}</td>
        <td>{config.data_type}</td>
        <td>{providerFormatter(config.providers[0])}</td>
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
        Add custom field
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

export default withLocation(withTelemetry(FieldsList));
