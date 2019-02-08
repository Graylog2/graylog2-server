import PropTypes from 'prop-types';
import React from 'react';
import naturalSort from 'javascript-natural-sort';

import { Row, Col } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import { DataAdapterForm } from 'components/lookup-tables';
import { PluginStore } from 'graylog-web-plugin/plugin';
import ObjectUtils from 'util/ObjectUtils';

class DataAdapterCreate extends React.Component {
  static propTypes = {
    saved: PropTypes.func.isRequired,
    types: PropTypes.object.isRequired,
    validate: PropTypes.func,
    validationErrors: PropTypes.object,
  };

  static defaultProps = {
    validate: null,
    validationErrors: {},
  };

  state = {
    dataAdapter: undefined,
    type: undefined,
  };

  _onTypeSelect = (adapterType) => {
    this.setState({
      type: adapterType,
      dataAdapter: {
        id: null,
        title: '',
        name: '',
        description: '',
        config: ObjectUtils.clone(this.props.types[adapterType].default_config),
      },
    });
  };

  render() {
    const adapterPlugins = {};
    PluginStore.exports('lookupTableAdapters').forEach((p) => {
      adapterPlugins[p.type] = p;
    });

    const sortedAdapters = Object.keys(this.props.types).map((key) => {
      const type = this.props.types[key];
      if (adapterPlugins[type.type] === undefined) {
        console.error(`Plugin component for data adapter type ${type.type} is missing - invalid or missing plugin?`);
        return { value: type.type, disabled: true, label: `${type.type} - missing or invalid plugin` };
      }
      return { value: type.type, label: adapterPlugins[type.type].displayName };
    }).sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));

    return (<div>
      <Row className="content">
        <Col lg={8}>
          <form className="form form-horizontal" onSubmit={() => {}}>
            <Input id="data-adapter-type-select"
                   label="Data Adapter Type"
                   required
                   autoFocus
                   help="The type of data adapter to configure."
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <Select placeholder="Select Data Adapter Type"
                      clearable={false}
                      options={sortedAdapters}
                      matchProp="label"
                      onChange={this._onTypeSelect}
                      value={null} />
            </Input>
          </form>
        </Col>
      </Row>
      {this.state.dataAdapter && (
        <Row className="content">
          <Col lg={12}>
            <h3>Configure Adapter</h3>
            <DataAdapterForm dataAdapter={this.state.dataAdapter}
                             type={this.state.type}
                             create
                             validate={this.props.validate}
                             validationErrors={this.props.validationErrors}
                             saved={this.props.saved} />
          </Col>
        </Row>
      )}
    </div>);
  }
}


export default DataAdapterCreate;
