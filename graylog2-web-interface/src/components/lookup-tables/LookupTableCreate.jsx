import React, { PropTypes } from 'react';
import naturalSort from 'javascript-natural-sort';

import { Row, Col } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import { LookupTableForm } from 'components/lookup-tables';
import { PluginStore } from 'graylog-web-plugin/plugin';
import ObjectUtils from 'util/ObjectUtils';

const LookupTableCreate = React.createClass({

  propTypes: {
    saved: PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      cache: undefined,
      type: undefined,
    };
  },

  render() {
    return (
      <div>
        <Row className="content">
          <Col lg={8}>
            <LookupTableForm saved={this.props.saved} create />
          </Col>
        </Row>
      </div>
    );
  },

});

export default LookupTableCreate;
