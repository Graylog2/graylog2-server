import React, { PropTypes } from 'react';
import { Row, Col } from 'react-bootstrap';

import SupportLink from 'components/support/SupportLink';

const PageHeader = React.createClass({
  propTypes: {
    title: PropTypes.oneOfType([PropTypes.node, PropTypes.string]).isRequired,
    children: PropTypes.oneOfType([PropTypes.array, PropTypes.node]),
  },
  render() {
    const children = (this.props.children !== undefined && this.props.children.length !== undefined ? this.props.children : [this.props.children]);
    return (
      <div>
        <Row className="content content-head">
          <Col sm={12}>
            {children[2] &&
            <div className="actions-lg visible-lg visible-md">
              <div className="actions-container">
                {children[2]}
              </div>
            </div>
            }

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
            <Col sm={12} lgHidden mdHidden className="actions-sm">
              {children[2]}
            </Col>
          }
        </Row>
      </div>
    );
  },
});

export default PageHeader;
