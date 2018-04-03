import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import Immutable from 'immutable';

import { Button, Col, Row } from 'react-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';

const DashboardContainer = createReactClass({
  propTypes: {
    view: PropTypes.instanceOf(Immutable.Map).isRequired,
    toggle: PropTypes.func,
  },

  getDefaultProps() {
    return {
      toggle: () => {},
    };
  },

  getInitialState() {
    return {};
  },

  render() {
    return (
      <DocumentTitle title="Dashboard Title">
        <span>
          <PageHeader title="Dashboard Title">
            <span>
               Dashboard description.
            </span>

            {null}

            <span>
              <Button onClick={this.props.toggle} >Queries</Button>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <pre>
                {JSON.stringify(this.props, null, 2)}
              </pre>
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default DashboardContainer;
