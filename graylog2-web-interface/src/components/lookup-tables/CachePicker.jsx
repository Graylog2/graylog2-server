import React, { PropTypes } from 'react';
import naturalSort from 'javascript-natural-sort';

import { PluginStore } from 'graylog-web-plugin/plugin';
import { Input } from 'components/bootstrap';
import { Select } from 'components/common';

const CachePicker = React.createClass({

  propTypes: {
    onSelect: PropTypes.func.isRequired,
    selectedId: PropTypes.string,
    caches: PropTypes.array,
    pagination: PropTypes.object,
  },

  getDefaultProps() {
    return {
      selectedId: null,
      caches: [],
      pagination: {},
    };
  },

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
        <Input label="Cache"
               required
               autoFocus
               help="Select an existing cache"
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9">
          <Select placeholder="Select a cache"
                  clearable={false}
                  options={sortedCaches}
                  matchProp="value"
                  onValueChange={this.props.onSelect}
                  value={this.props.selectedId} />
        </Input>
      </fieldset>
    );
  },
});

export default CachePicker;
