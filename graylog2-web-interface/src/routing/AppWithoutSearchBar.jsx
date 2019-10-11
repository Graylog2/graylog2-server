import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'components/graylog';
import Footer from 'components/layout/Footer';

const AppWithoutSearchBar = (props) => {
  const { children } = props;
  return (
    <div className="container-fluid">
      <Row id="main-row">
        <Col md={12} id="main-content">
          {children}
        </Col>
      </Row>
      <Footer />
    </div>
  );
};

AppWithoutSearchBar.propTypes = {
  children: PropTypes.node.isRequired,
};

export default AppWithoutSearchBar;
