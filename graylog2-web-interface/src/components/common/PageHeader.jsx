import React, { PropTypes } from 'react';
import { Row, Col, Label, OverlayTrigger, Tooltip } from 'react-bootstrap';

import SupportLink from 'components/support/SupportLink';

const PageHeader = React.createClass({
  propTypes: {
    title: PropTypes.oneOfType([PropTypes.node, PropTypes.string]).isRequired,
    children: PropTypes.oneOfType([PropTypes.array, PropTypes.node]),
    experimental: PropTypes.bool,
    subpage: PropTypes.bool,
  },
  getDefaultProps() {
    return {
      experimental: false,
      subpage: false,
    };
  },
  render() {
    const children = (this.props.children !== undefined && this.props.children.length !== undefined ? this.props.children : [this.props.children]);

    let experimentalLabel;
    if (this.props.experimental) {
      experimentalLabel = (
        <span style={{ cursor: 'help', marginLeft: 5, fontSize: 14, lineHeight: '20px', verticalAlign: 'text-top' }}>
          <OverlayTrigger placement="bottom" overlay={<Tooltip id="experimental">This feature of Graylog is new and should be considered experimental.</Tooltip>}>
            <Label bsStyle="primary">Experimental</Label>
          </OverlayTrigger>
        </span>
      );
    }

    const topLevelClassNames = this.props.subpage ? 'content-head' : 'content content-head';
    return (
      <div>
        <Row className={topLevelClassNames}>
          <Col sm={12}>
            {children[2] &&
            <div className="actions-lg visible-lg visible-md">
              <div className="actions-container">
                {children[2]}
              </div>
            </div>
            }

            <h1>
              {this.props.title} <small>{experimentalLabel}</small>
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
