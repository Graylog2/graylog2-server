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

import Select from 'components/common/Select';
import { defaultCompare } from 'views/logic/DefaultCompare';

const VisualizationTypeSelect = ({ onChange, value }) => {
  const visualizationTypes = PluginStore.exports('visualizationTypes')
    .sort((v1, v2) => defaultCompare(v1.displayName, v2.displayName))
    .map((viz) => ({ label: viz.displayName, value: viz.type }));

  return (
    <Select placeholder="Visualization type"
            options={visualizationTypes}
            onChange={onChange}
            clearable={false}
            value={value} />
  );
};

VisualizationTypeSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
};

export default VisualizationTypeSelect;
