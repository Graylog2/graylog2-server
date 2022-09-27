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
import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';

import { Row, Col } from 'components/bootstrap';

const StyledListItem = styled.li(({ theme }) => css`
  display: block;
  padding: 15px 0;

  h2 .label {
    margin-left: 5px;
    line-height: 1;
    vertical-align: baseline;
  }

  .item-description {
    min-height: 17px;
    margin: 5px 0;
  }

  .item-actions > .btn,
  .item-actions > .btn-group,
  .item-actions > span > .btn {
    margin-left: 5px;
    margin-bottom: 5px;
  }

  &:not(:last-child) {
    border-bottom: 1px solid ${theme.colors.gray[90]}
  }
`);

type Props = {
  title: React.ReactNode,
  titleSuffix?: React.ReactNode,
  description?: React.ReactNode,
  actions?: React.ReactNode | Array<React.ReactNode>,
  contentRow?: React.ReactNode,
}

/**
 * Component that let you render an entity item using a similar look and feel as other entities in Graylog.
 * This component is meant to use alongside `EntityList`. Look there for an example of how to use this component.
 */
const EntityListItem = ({ actions, contentRow, description, title, titleSuffix }: Props) => {
  const wrappedTitleSuffix = titleSuffix ? <small>{titleSuffix}</small> : null;
  const actionsContainer = (
    <div className="item-actions text-right">
      {actions}
    </div>
  );

  return (
    <StyledListItem>
      <Row className="row-sm">
        <Col md={12}>
          <div className="pull-right hidden-xs">
            {actionsContainer}
          </div>
          <h2>{title} {wrappedTitleSuffix}</h2>
          {description
          && (
            <div className="item-description">
              <span>{description}</span>
            </div>
          )}
        </Col>

        <Col sm={12} lgHidden mdHidden smHidden>
          {actionsContainer}
        </Col>
      </Row>

      {contentRow && (
      <Row className="row-sm">
        {contentRow}
      </Row>
      )}
    </StyledListItem>
  );
};

EntityListItem.propTypes = {
  /** Entity's title. */
  title: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
  /** Text to append to the title. Usually the type or a short description. */
  titleSuffix: PropTypes.any,
  /** Description of the element, which can accommodate more text than `titleSuffix`. */
  description: PropTypes.any,
  /** Action buttons or menus shown on the right side of the entity item container. */
  actions: PropTypes.oneOfType([PropTypes.array, PropTypes.node]),
  /**
   * Add any content that is related to the entity and needs more space to be displayed. This is mostly use
   * to show configuration options.
   */
  contentRow: PropTypes.node,
};

EntityListItem.defaultProps = {
  actions: undefined,
  contentRow: undefined,
  description: undefined,
  titleSuffix: undefined,
};

export default EntityListItem;
