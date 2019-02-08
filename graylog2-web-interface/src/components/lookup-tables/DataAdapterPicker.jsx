import PropTypes from 'prop-types';
import React from 'react';
import naturalSort from 'javascript-natural-sort';

import { PluginStore } from 'graylog-web-plugin/plugin';
import { Input } from 'components/bootstrap';
import { Select } from 'components/common';

class DataAdapterPicker extends React.Component {
  static propTypes = {
    onSelect: PropTypes.func.isRequired,
    selectedId: PropTypes.string,
    dataAdapters: PropTypes.array,
    pagination: PropTypes.object,
  };

  static defaultProps = {
    selectedId: null,
    dataAdapters: [],
    pagination: {},
  };

  render() {
    const adapterPlugins = {};
    PluginStore.exports('lookupTableAdapters').forEach((p) => {
      adapterPlugins[p.type] = p;
    });

    const sortedAdapters = this.props.dataAdapters.map((adapter) => {
      return { value: adapter.id, label: `${adapter.title} (${adapter.name})` };
    }).sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));

    return (
      <fieldset>
        <Input id="data-adapter-select"
               label="Data Adapter"
               required
               autoFocus
               help="Select an existing data adapter"
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9">
          <Select placeholder="Select a data adapter"
                  clearable={false}
                  options={sortedAdapters}
                  matchProp="label"
                  onChange={this.props.onSelect}
                  value={this.props.selectedId} />
        </Input>
      </fieldset>
    );
  }
}

export default DataAdapterPicker;
