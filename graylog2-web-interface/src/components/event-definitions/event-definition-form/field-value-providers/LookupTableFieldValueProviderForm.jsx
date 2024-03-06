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
import cloneDeep from 'lodash/cloneDeep';

import { LookupTableFields } from 'components/lookup-tables';

class LookupTableFieldValueProviderForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static type = 'lookup-v1';

  static defaultConfig = {
    table_name: '',
    key_field: '',
  };

  static requiredFields = [
    'table_name',
    'key_field',
  ];

  propagateChanges = (key, value) => {
    const { config, onChange } = this.props;
    const nextProviders = cloneDeep(config.providers);
    const lookupProvider = nextProviders.find((provider) => provider.type === LookupTableFieldValueProviderForm.type);

    lookupProvider[key] = value;
    onChange({ ...config, providers: nextProviders });
  };

  handleSelectChange = (key) => (nextLookupTable) => {
    this.propagateChanges(key, nextLookupTable);
  };

  render() {
    const { config, validation } = this.props;
    const provider = config.providers.find((p) => p.type === LookupTableFieldValueProviderForm.type);

    return (
      <LookupTableFields onTableNameChange={this.handleSelectChange('table_name')}
                         onKeyChange={this.handleSelectChange('key_field')}
                         selectedTableName={provider.table_name}
                         selectedKeyName={provider.key_field}
                         nameValidation={validation.errors.table_name}
                         keyValidation={validation.errors.key_field} />
    );
  }
}

export default LookupTableFieldValueProviderForm;
