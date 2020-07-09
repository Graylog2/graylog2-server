import PropTypes from 'prop-types';
import React from 'react';
import lodash from 'lodash';
import styled, { css } from 'styled-components';

import { Col, Label, OverlayTrigger, Tooltip } from 'components/graylog';
import ContentHeadRow from 'components/common/ContentHeadRow';
import SupportLink from 'components/support/SupportLink';

const LifecycleIndicator = styled.span(({ theme }) => css`
  cursor: help;
  margin-left: 5px;
  font-size: ${theme.fonts.size.body};
  line-height: 20px;
  vertical-align: text-top;
`);

const H1 = styled.h1`
  margin-bottom: 0.2em;
`;

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
    const { lifecycle, lifecycleMessage } = this.props;

    if (lifecycle === undefined) {
      return null;
    }

    const label = lodash.upperFirst(lifecycle);
    const defaultMessage = lifecycle === 'experimental' ? LIFECYCLE_DEFAULT_MESSAGES.experimental : LIFECYCLE_DEFAULT_MESSAGES.legacy;
    const tooltip = <Tooltip id={lifecycle}>{lifecycleMessage || defaultMessage}</Tooltip>;

    return (
      <LifecycleIndicator>
        <OverlayTrigger placement="bottom" overlay={tooltip}>
          <Label bsStyle="primary">{label}</Label>
        </OverlayTrigger>
      </LifecycleIndicator>
    );
  };

  render() {
    const { children: childList, subpage, title } = this.props;
    const children = (childList !== undefined && childList.length !== undefined ? childList : [childList]);

    const topLevelClassNames = subpage ? '' : 'content';

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
            )}

            <H1>
              {title} <small>{this.renderLifecycleIndicator()}</small>
            </H1>
            {children[0]
            && (
            <p className="description">
              {children[0]}
            </p>
            )}

            {children[1]
            && (
            <SupportLink>
              {children[1]}
            </SupportLink>
            )}
          </Col>

          {children[2]
            && (
            <Col sm={12} lgHidden mdHidden className="actions-sm">
              {children[2]}
            </Col>
            )}
        </ContentHeadRow>
      </div>
    );
  }
}

export default PageHeader;
