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
import naturalSort from 'javascript-natural-sort';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';

class CachePicker extends React.Component {
  static propTypes = {
    onSelect: PropTypes.func.isRequired,
    selectedId: PropTypes.string,
    caches: PropTypes.array,
    pagination: PropTypes.object,
  };

  static defaultProps = {
    selectedId: null,
    caches: [],
    pagination: {},
  };

  render() {
    const cachePlugins = {};

    PluginStore.exports('lookupTableCaches').forEach((p) => {
      cachePlugins[p.type] = p;
    });

    const sortedCaches = this.props.caches.map((cache) => {
      return { value: cache.id, label: `${cache.title} (${cache.name})` };
    }).sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));

    return (
      <fieldset>
        <Input id="cache-select"
               label="Cache"
               required
               autoFocus
               help="Select an existing cache"
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9">
          <Select placeholder="Select a cache"
                  clearable={false}
                  options={sortedCaches}
                  matchProp="label"
                  onChange={this.props.onSelect}
                  value={this.props.selectedId} />
        </Input>
      </fieldset>
    );
  }
}

export default CachePicker;
