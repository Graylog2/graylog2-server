import React, { PropTypes } from 'react';
import naturalSort from 'javascript-natural-sort';

import { Row, Col } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import { CacheForm } from 'components/lookup-tables';
import { PluginStore } from 'graylog-web-plugin/plugin';
import ObjectUtils from 'util/ObjectUtils';

const CacheCreate = React.createClass({

  propTypes: {
    saved: PropTypes.func.isRequired,
    types: PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      cache: undefined,
      type: undefined,
    };
  },

  _onTypeSelect(cacheType) {
    this.setState({
      type: cacheType,
      cache: {
        id: null,
        title: '',
        name: '',
        description: '',
        config: ObjectUtils.clone(this.props.types[cacheType].default_config),
      },
    });
  },

  render() {
    const cachePlugins = {};
    PluginStore.exports('lookupTableCaches').forEach((p) => {
      cachePlugins[p.type] = p;
    });

    const sortedCaches = Object.keys(this.props.types).map((key) => {
      const type = this.props.types[key];
      return { value: type.type, label: cachePlugins[type.type].displayName };
    }).sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));

    return (<div>
      <Row className="content">
        <Col lg={8}>
          <form className="form form-horizontal" onSubmit={() => {}}>
            <Input label="Cache Type"
                   required
                   autoFocus
                   help="The type of cache to configure."
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <Select placeholder="Select Cache Type"
                      clearable={false}
                      options={sortedCaches}
                      matchProp="value"
                      onValueChange={this._onTypeSelect}
                      value={null} />
            </Input>
          </form>
        </Col>
      </Row>
      {this.state.cache && (
        <Row className="content">
          <Col lg={12}>
            <h3>Configure Cache</h3>
            <CacheForm cache={this.state.cache} type={this.state.type} create saved={this.props.saved} />
          </Col>
        </Row>
      )}
    </div>);
  },
});


export default CacheCreate;
