import PropTypes from 'prop-types';
import React from 'react';
import { Row, Col } from 'react-bootstrap';

class AppWithoutSearchBar extends React.Component {
  static propTypes = {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  };

  render() {
    return (
      <div className="container-fluid">
        <Row id="main-row">
          <Col md={12} id="main-content">
            {this.props.children}
          </Col>
        </Row>
      </div>
    );
  }
}

export default AppWithoutSearchBar;
