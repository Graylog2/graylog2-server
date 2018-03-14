import PropTypes from 'prop-types';
import React from 'react';
import { Row, Col, Label, OverlayTrigger, Tooltip } from 'react-bootstrap';

import SupportLink from 'components/support/SupportLink';

/**
 * Component that renders a page header, with a title and some optional content.
 * This ensures all pages look and feel the same way across the product, so
 * please use it in your pages.
 */
class PageHeader extends React.Component {
  static propTypes = {
    /** Page header heading. */
    title: PropTypes.oneOfType([PropTypes.node, PropTypes.string]).isRequired,
    /**
     * One or more children, they will be used in the header in this order:
     *  1. Page description
     *  2. Support information or link
     *  3. Action buttons
     *
     * Please see the examples to see how to use this in practice.
     */
    children: PropTypes.oneOfType([PropTypes.array, PropTypes.node]),
    /** Flag that specifies if the page is experimental or not. */
    experimental: PropTypes.bool,
    /** Specifies if the page header is children of a content `Row` or not. */
    subpage: PropTypes.bool,
  };

  static defaultProps = {
    experimental: false,
    subpage: false,
  };

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
  }
}

export default PageHeader;
