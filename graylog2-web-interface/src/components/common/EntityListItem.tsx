import React from 'react';
import styled, { css } from 'styled-components';

import { Row, Col } from 'components/bootstrap';

const StyledListItem = styled.li(({ theme }) => css`
  display: block;
  padding: 15px 0;

  h2 {
    font-family: ${theme.fonts.family.body};
  }

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
    border-bottom: 1px solid ${theme.colors.gray[90]};
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
          {description && (
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

export default EntityListItem;
