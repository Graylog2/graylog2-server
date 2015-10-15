import React from 'react';
import { Row, Col } from 'react-bootstrap';

import SupportLink from 'components/support/SupportLink';

const PageHeader = React.createClass({
  propTypes: {
    title: React.PropTypes.string.isRequired,
  },
  render() {
    return (
      <span>
        <Row className="content content-head">
          <Col md={10}>
            <h1>
              {this.props.title}
            </h1>
            <p className="description">
              {this.props.children[0]}
            </p>

            <SupportLink>
              {this.props.children[1]}
            </SupportLink>
          </Col>
          <Col md={2} style={{textAlign: 'center', marginTop: '35px'}}>
            {this.props.children[2]}
          </Col>
        </Row>
      </span>
    );
  }
});

export default PageHeader;
