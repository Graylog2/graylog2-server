import PropTypes from 'prop-types';
import React from 'react';
import { Row, Col, Label, OverlayTrigger, Tooltip } from 'components/graylog';
import { ContentHeadRow } from 'components/common';
import lodash from 'lodash';

import SupportLink from 'components/support/SupportLink';

const LIFECYCLE_DEFAULT_MESSAGES = {
  experimental: 'This Graylog feature is new and should be considered experimental.',
  legacy: 'This feature has been discontinued and will be removed in a future Graylog version.',
};

/**
 * Component that renders a page header, with a title and some optional content.
 * This ensures all pages look and feel the same way across the product, so
 * please use it in your pages.
 */
class PageHeader extends React.Component {
  static propTypes = {
    /** Page header heading. */
    title: PropTypes.node.isRequired,
    /**
     * One or more children, they will be used in the header in this order:
     *  1. Page description
     *  2. Support information or link
     *  3. Action buttons
     *
     * Please see the examples to see how to use this in practice.
     */
    children: PropTypes.node,
    /** Indicates the lifecycle of the current page, which will display an indicator right next to the page title. */
    lifecycle: PropTypes.oneOf(['experimental', 'legacy']),
    /** Text to customize the default message for the given lifecycle. */
    lifecycleMessage: PropTypes.node,
    /** Specifies if the page header is children of a content `Row` or not. */
    subpage: PropTypes.bool,
  };

  static defaultProps = {
    lifecycle: undefined,
    lifecycleMessage: undefined,
    subpage: false,
    children: [],
  };

  renderLifecycleIndicator = () => {
    if (this.props.lifecycle === undefined) {
      return null;
    }

    const label = lodash.upperFirst(this.props.lifecycle);
    const defaultMessage = this.props.lifecycle === 'experimental' ? LIFECYCLE_DEFAULT_MESSAGES.experimental : LIFECYCLE_DEFAULT_MESSAGES.legacy;
    const tooltip = <Tooltip id={this.props.lifecycle}>{this.props.lifecycleMessage || defaultMessage}</Tooltip>;

    return (
      <span style={{ cursor: 'help', marginLeft: 5, fontSize: 14, lineHeight: '20px', verticalAlign: 'text-top' }}>
        <OverlayTrigger placement="bottom" overlay={tooltip}>
          <Label bsStyle="primary">{label}</Label>
        </OverlayTrigger>
      </span>
    );
  };

  render() {
    const children = (this.props.children !== undefined && this.props.children.length !== undefined ? this.props.children : [this.props.children]);

    const topLevelClassNames = this.props.subpage ? '' : 'content';
    return (
      <div>
        <ContentHeadRow className={topLevelClassNames}>
          <Col sm={12}>
            {children[2]
            && (
            <div className="actions-lg visible-lg visible-md">
              <div className="actions-container">
                {children[2]}
              </div>
            </div>
            )
            }

            <h1>
              {this.props.title} <small>{this.renderLifecycleIndicator()}</small>
            </h1>
            {children[0]
            && (
            <p className="description">
              {children[0]}
            </p>
            )
            }

            {children[1]
            && (
            <SupportLink>
              {children[1]}
            </SupportLink>
            )
            }
          </Col>

          {children[2]
            && (
            <Col sm={12} lgHidden mdHidden className="actions-sm">
              {children[2]}
            </Col>
            )
          }
        </ContentHeadRow>
      </div>
    );
  }
}

export default PageHeader;
