import React from 'react';
import Reflux from 'reflux';
import naturalSort from 'javascript-natural-sort';

import Routes from 'routing/Routes';

import { Row, Col } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import { Spinner, Select } from 'components/common';
import { DataAdapterForm } from 'components/lookup-tables';
import { PluginStore } from 'graylog-web-plugin/plugin';
import CombinedProvider from 'injection/CombinedProvider';
import ObjectUtils from 'util/ObjectUtils';

const { LookupTableDataAdaptersStore, LookupTableDataAdaptersActions } = CombinedProvider.get(
  'LookupTableDataAdapters');

function filterTypes(types) {
  return types ? types.types : null;
}

const DataAdapterCreate = React.createClass({

  propTypes: {
    history: React.PropTypes.object.isRequired,
  },

  mixins: [
    Reflux.connectFilter(LookupTableDataAdaptersStore, 'types', filterTypes),
  ],

  getInitialState() {
    return {
      dataAdapter: undefined,
    };
  },

  componentDidMount() {
    LookupTableDataAdaptersActions.getTypes();
    const plugins = PluginStore.exports('lookupTableAdapters');
    plugins.forEach((p) => {
      this.adapterPlugins[p.type] = p;
    });
  },

  adapterPlugins: {},

  _onTypeSelect(adapterType) {
    this.setState({
      type: adapterType,
      dataAdapter: {
        id: null,
        title: '',
        name: '',
        description: '',
        config: ObjectUtils.clone(this.state.types[adapterType].default_config),
      },
    });
  },

  _saved() {
    this.props.history.pushState(null, Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW);
  },

  render() {
    if (!this.state.types) {
      return <Spinner text="Loading available data adapter types" />;
    }
    //

    const sortedAdapters = Object.keys(this.state.types).map((key) => {
      const type = this.state.types[key];
      return { value: type.type, label: this.adapterPlugins[type.type].displayName };
    }).sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));

    return (<div>
      <Row className="content">
        <Col lg={8}>
          <form className="form form-horizontal" onSubmit={() => {}}>
            <Input label="Data Adapter Type"
                   required
                   autoFocus
                   help="The type of data adapter to configure."
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <Select placeholder="Select Data Adapter Type"
                      clearable={false}
                      options={sortedAdapters}
                      matchProp="value"
                      onValueChange={this._onTypeSelect}
                      value={null} />
            </Input>
          </form>
        </Col>
      </Row>
      {this.state.dataAdapter && (
        <Row className="content">
          <Col lg={8}>
            <h3>Configure Adapter</h3>
            <DataAdapterForm dataAdapter={this.state.dataAdapter} type={this.state.type} create saved={this._saved} />
          </Col>
        </Row>
      )}
    </div>);
  },
});

export default DataAdapterCreate;
