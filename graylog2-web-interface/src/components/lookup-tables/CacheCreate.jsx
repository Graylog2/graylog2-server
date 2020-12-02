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

import { Row, Col } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import { CacheForm } from 'components/lookup-tables';
import ObjectUtils from 'util/ObjectUtils';

class CacheCreate extends React.Component {
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
    cache: undefined,
    type: undefined,
  };

  _onTypeSelect = (cacheType) => {
    const { types } = this.props;

    this.setState({
      type: cacheType,
      cache: {
        id: null,
        title: '',
        name: '',
        description: '',
        config: ObjectUtils.clone(types[cacheType].default_config),
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
    const { type, cache } = this.state;
    const cachePlugins = {};

    PluginStore.exports('lookupTableCaches').forEach((p) => {
      cachePlugins[p.type] = p;
    });

    const sortedCaches = Object.keys(types).map((key) => {
      const typeItem = types[key];

      return { value: typeItem.type, label: cachePlugins[typeItem.type].displayName };
    }).sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));

    return (
      <div>
        <Row className="content">
          <Col lg={8}>
            <form className="form form-horizontal" onSubmit={() => {}}>
              <Input id="cache-type-select"
                     label="Cache Type"
                     required
                     autoFocus
                     help="The type of cache to configure."
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9">
                <Select placeholder="Select Cache Type"
                        clearable={false}
                        options={sortedCaches}
                        matchProp="label"
                        onChange={this._onTypeSelect}
                        value={null} />
              </Input>
            </form>
          </Col>
        </Row>
        {cache && (
        <Row className="content">
          <Col lg={12}>
            <CacheForm cache={cache}
                       type={type}
                       title="Configure Cache"
                       create
                       saved={saved}
                       validationErrors={validationErrors}
                       validate={validate} />
          </Col>
        </Row>
        )}
      </div>
    );
  }
}

export default CacheCreate;
