import PropTypes from 'prop-types';
import React from 'react';
import naturalSort from 'javascript-natural-sort';

import { Row, Col } from 'components/graylog';
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
    const { types } = this.props;
    this.setState({
      type: adapterType,
      dataAdapter: {
        id: null,
        title: '',
        name: '',
        description: '',
        config: ObjectUtils.clone(types[adapterType].default_config),
      },
    });
  };

  render() {
    const {
      types,
      validate,
      validationErrors,
      saved,
    } = this.props;
    const { type, dataAdapter } = this.state;
    const adapterPlugins = {};
    PluginStore.exports('lookupTableAdapters').forEach((p) => {
      adapterPlugins[p.type] = p;
    });

    const sortedAdapters = Object.keys(types).map((key) => {
      const typeItem = types[key];
      if (adapterPlugins[typeItem.type] === undefined) {
        // eslint-disable-next-line no-console
        console.error(`Plugin component for data adapter type ${typeItem.type} is missing - invalid or missing plugin?`);
        return { value: typeItem.type, disabled: true, label: `${typeItem.type} - missing or invalid plugin` };
      }
      return { value: typeItem.type, label: adapterPlugins[typeItem.type].displayName };
    }).sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));

    return (
      <div>
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
        {dataAdapter && (
        <Row className="content">
          <Col lg={12}>
            <DataAdapterForm dataAdapter={dataAdapter}
                             type={type}
                             create
                             title="Configure Adapter"
                             validate={validate}
                             validationErrors={validationErrors}
                             saved={saved} />
          </Col>
        </Row>
        )}
      </div>
    );
  }
}


export default DataAdapterCreate;
