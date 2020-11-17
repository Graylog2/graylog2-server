/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import styled, { css, type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { Col, Label, OverlayTrigger, Tooltip } from 'components/graylog';
import ContentHeadRow from 'components/common/ContentHeadRow';
import SupportLink from 'components/support/SupportLink';

const LifecycleIndicator: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  cursor: help;
  margin-left: 5px;
  font-size: ${theme.fonts.size.body};
  line-height: 20px;
  vertical-align: text-top;
`);

const H1 = styled.h1`
  margin-bottom: 0.2em;
`;

const ActionsSM = styled.div`
  > * {
    display: inline-block;
    vertical-align: top;
  }
  > :not(:last-child) {
    margin-right: 5px;
  }
`;

const LIFECYCLE_DEFAULT_MESSAGES = {
  experimental: 'This Graylog feature is new and should be considered experimental.',
  legacy: 'This feature has been discontinued and will be removed in a future Graylog version.',
};

type Props = {
  title: React.Node,
  children: Array<React.Node>,
  subactions?: React.Node,
  lifecycle?: 'experimental' | 'legacy',
  lifecycleMessage?: React.Node,
  subpage: boolean,
};

/**
 * Component that renders a page header, with a title and some optional content.
 * This ensures all pages look and feel the same way across the product, so
 * please use it in your pages.
 */
class PageHeader extends React.Component<Props> {
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
    /** Section for subactions like create or edit */
    subactions: PropTypes.node,
    /** Indicates the lifecycle of the current page, which will display an indicator right next to the page title. */
    lifecycle: PropTypes.oneOf(['experimental', 'legacy']),
    /** Text to customize the default message for the given lifecycle. */
    lifecycleMessage: PropTypes.node,
    /** Specifies if the page header is children of a content `Row` or not. */
    subpage: PropTypes.bool,
  };

  static defaultProps = {
    children: [],
    lifecycle: undefined,
    lifecycleMessage: undefined,
    subactions: undefined,
    subpage: false,
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
    const { children: childList, subpage, title, subactions } = this.props;
    const children = (childList !== undefined && childList.length !== undefined ? childList : [childList]);

    const topLevelClassNames = subpage ? '' : 'content';

    return (
      <div>
        <ContentHeadRow className={topLevelClassNames}>
          <Col sm={12}>
            {children[2] && (
              <div className="actions-lg visible-lg visible-md">
                <div className="actions-container">
                  {children[2]}
                </div>
              </div>
            )}

            <H1>
              {title} <small>{this.renderLifecycleIndicator()}</small>
            </H1>

            {children[0] && (
              <p className="description">
                {children[0]}
              </p>
            )}

            {children[1] && (
              <SupportLink>
                {children[1]}
              </SupportLink>
            )}

            {subactions && (
              <div className="pull-right visible-lg visible-md">
                {subactions}
              </div>
            )}
          </Col>

          {children[2] && (
            <Col sm={12} lgHidden mdHidden className="actions-sm">
              <ActionsSM>
                {children[2]}{subactions}
              </ActionsSM>
            </Col>
          )}
        </ContentHeadRow>
      </div>
    );
  }
}

export default PageHeader;
