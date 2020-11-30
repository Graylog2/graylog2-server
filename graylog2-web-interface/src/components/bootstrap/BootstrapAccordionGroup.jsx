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

class BootstrapAccordionGroup extends React.Component {
  static propTypes = {
    name: PropTypes.string,
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  };

  render() {
    let name;
    let id;

    if (this.props.name) {
      name = this.props.name;
      id = name.replace(/[^0-9a-zA-Z]/g, '-').toLowerCase();
    }

    return (
      <div className="panel panel-default">
        <div className="panel-heading">
          <h4 className="panel-title">
            <a href={`#${id}`} data-parent="#bundles" data-toggle="collapse" className="collapsed">{name}</a>
          </h4>
        </div>
        <div className="panel-collapse collapse" id={id}>
          <div className="panel-body">
            {this.props.children}
          </div>
        </div>
      </div>
    );
  }
}

export default BootstrapAccordionGroup;
