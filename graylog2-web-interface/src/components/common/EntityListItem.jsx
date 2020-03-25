import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Row, Col } from 'components/graylog';
import Icon from './Icon';

const StyledListItem = styled.li(({ theme }) => `
  display: block;
  padding: 15px 0;

  h2 .label {
    margin-left: 5px;
    line-height: 2;
    vertical-align: bottom;
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
    border-bottom: 1px solid ${theme.color.variant.light.info};
  }
`);

/**
 * Component that let you render an entity item using a similar look and feel as other entities in Graylog.
 * This component is meant to use alongside `EntityList`. Look there for an example of how to use this component.
 */
class EntityListItem extends React.Component {
  static propTypes = {
    /** Entity's title. */
    title: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
    /** Text to append to the title. Usually the type or a short description. */
    titleSuffix: PropTypes.any,
    /** Description of the element, which can accommodate more text than `titleSuffix`. */
    description: PropTypes.any,
    /** Action buttons or menus shown on the right side of the entity item container. */
    actions: PropTypes.oneOfType([PropTypes.array, PropTypes.node]),
    /** Flag that controls whether the content pack marker will be shown next to the description or not. */
    createdFromContentPack: PropTypes.bool,
    /**
     * Add any content that is related to the entity and needs more space to be displayed. This is mostly use
     * to show configuration options.
     */
    contentRow: PropTypes.node.isRequired,
  };

  static defaultProps = {
    actions: undefined,
    createdFromContentPack: false,
    description: undefined,
    titleSuffix: undefined,
  };

  render() {
    const {
      actions,
      contentRow,
      createdFromContentPack,
      description,
      title,
      titleSuffix,
    } = this.props;
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
            {(createdFromContentPack || description)
              && (
              <div className="item-description">
                {createdFromContentPack
                && <span><Icon name="cube" title="Created from content pack" />&nbsp;</span>}
                <span>{description}</span>
              </div>
              )}
          </Col>

          <Col sm={12} lgHidden mdHidden smHidden>
            {actionsContainer}
          </Col>
        </Row>

        <Row className="row-sm">
          {contentRow}
        </Row>
      </StyledListItem>
    );
  }
}

export default EntityListItem;
