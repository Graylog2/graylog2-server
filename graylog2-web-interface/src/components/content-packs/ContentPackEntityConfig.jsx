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
import naturalSort from 'javascript-natural-sort';

import DataTable from 'components/common/DataTable';
import ValueReferenceData from 'util/ValueReferenceData';

class ContentPackEntityConfig extends React.Component {
  static propTypes = {
    entity: PropTypes.object.isRequired,
    appliedParameter: PropTypes.array,
    parameters: PropTypes.array,
  };

  static defaultProps = {
    appliedParameter: [],
    parameters: [],
  };

  _getParameterForConfigKey = (configKey) => {
    const paramMapIndex = this.props.appliedParameter.findIndex((paramMap) => paramMap.configKey === configKey);

    if (paramMapIndex < 0) {
      return undefined;
    }

    const paramMap = this.props.appliedParameter[paramMapIndex];
    const paramIndex = this.props.parameters.findIndex((parameter) => parameter.name === paramMap.paramName);

    return this.props.parameters[paramIndex];
  };

  _configKeyRowFormatter = (paths) => {
    return (configKey) => {
      const path = paths[configKey];
      const parameter = this._getParameterForConfigKey(configKey);
      const type = parameter ? (<b>{`parameter (${parameter.type})`}</b>) : path.getValueType();
      const value = parameter ? (<b>{parameter.name}</b>) : path.getValue();

      return (
        <tr key={configKey}>
          <td>{configKey}</td>
          <td>{type}</td>
          <td>{value}</td>
        </tr>
      );
    };
  };

  render() {
    const entityData = new ValueReferenceData(this.props.entity.data);
    const configPaths = entityData.getPaths();
    const configKeys = Object.keys(configPaths).sort(naturalSort);

    return (
      <div>
        <DataTable id="entiy-config-list"
                   headers={['Config Key', 'Type', 'Value']}
                   filterKeys={[]}
                   rows={configKeys}
                   dataRowFormatter={this._configKeyRowFormatter(configPaths)} />
      </div>
    );
  }
}

export default ContentPackEntityConfig;
