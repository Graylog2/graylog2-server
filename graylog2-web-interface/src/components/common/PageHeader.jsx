import React, {PropTypes} from 'react';
import { Row, Col } from 'react-bootstrap';

import SupportLink from 'components/support/SupportLink';

const PageHeader = React.createClass({
  propTypes: {
    title: PropTypes.oneOfType([PropTypes.node, PropTypes.string]).isRequired,
    children: PropTypes.oneOfType([PropTypes.array, PropTypes.node]),
    titleSize: PropTypes.number,
    buttonSize: PropTypes.number,
    buttonStyle: PropTypes.object,
  },
  getDefaultProps() {
    return {
      titleSize: 10,
      buttonSize: 2,
      buttonStyle: {textAlign: 'center', marginTop: '35px'},
    };
  },
  render() {
    const children = (this.props.children !== undefined && this.props.children.length !== undefined ? this.props.children : [this.props.children]);
    return (
      <span>
        <Row className="content content-head">
          <Col md={this.props.titleSize}>
            <h1>
              {this.props.title}
            </h1>
            {children[0] &&
            <p className="description">
              {children[0]}
            </p>
            }

            {children[1] &&
            <SupportLink>
              {children[1]}
            </SupportLink>
            }
          </Col>
          {children[2] &&
          <Col md={this.props.buttonSize} style={this.props.buttonStyle}>
            {children[2]}
          </Col>
          }
        </Row>
      </span>
    );
  },
});

export default PageHeader;
