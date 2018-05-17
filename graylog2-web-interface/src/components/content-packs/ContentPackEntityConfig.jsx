import React from 'react';
import PropTypes from 'prop-types';

import DataTable from 'components/common/DataTable';
import ObjectUtils from 'util/ObjectUtils';

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
    const paramMapIndex = this.props.appliedParameter.findIndex(paramMap => paramMap.configKey === configKey);
    if (paramMapIndex < 0) {
      return undefined;
    }
    const paramMap = this.props.appliedParameter[paramMapIndex];
    const paramIndex = this.props.parameters.findIndex(parameter => parameter.name === paramMap.paramName);
    return this.props.parameters[paramIndex];
  };

  _configKeyRowFormatter = (configKey) => {
    const configMap = ObjectUtils.getValue(this.props.entity.data, configKey);
    const parameter = this._getParameterForConfigKey(configKey);
    const type = parameter ? (<b>{`parameter (${parameter.type})`}</b>) : configMap.type;
    const value = parameter ? (<b>{parameter.name}</b>) : configMap.value;
    return (
      <tr key={configKey}>
        <td>{configKey}</td>
        <td>{type}</td>
        <td>{value}</td>
      </tr>
    );
  };

  render() {
    const typeRegExp = RegExp(/\.type$/);
    const configKeys = ObjectUtils.getPaths(this.props.entity.data)
      .filter(configKey => typeRegExp.test(configKey))
      .map((configKey) => { return configKey.replace(typeRegExp, ''); });
    return (
      <div>
        <DataTable
          id="entiy-config-list"
          headers={['Config Key', 'Type', 'Value']}
          filterKeys={[]}
          rows={configKeys}
          dataRowFormatter={this._configKeyRowFormatter}
        />
      </div>
    );
  }
}

export default ContentPackEntityConfig;
