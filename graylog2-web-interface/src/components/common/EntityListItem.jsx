import PropTypes from 'prop-types';
import React from 'react';
import { Row, Col } from 'react-bootstrap';

/**
 * Component that let you render an entity item using a similar look and feel as other entities in Graylog.
 * This component is meant to use alongside `EntityList`. Look there for an example of how to use this component.
 */
class EntityListItem extends React.Component {
  static propTypes = {
    /** Entity's title. */
    title: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
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
    contentRow: PropTypes.node,
  };

  static defaultProps = {
    createdFromContentPack: false,
  };

  render() {
    let titleSuffix;
    if (this.props.titleSuffix) {
      titleSuffix = <small>{this.props.titleSuffix}</small>;
    }

    const actionsContainer = (
      <div className="item-actions text-right">
        {this.props.actions}
      </div>
    );

    return (
      <li className="entity-list-item">
        <Row className="row-sm">
          <Col md={12}>
            <div className="pull-right hidden-xs">
              {actionsContainer}
            </div>
            <h2>{this.props.title} {titleSuffix}</h2>
            {(this.props.createdFromContentPack || this.props.description) &&
              <div className="item-description">
                {this.props.createdFromContentPack &&
                <span><i className="fa fa-cube" title="Created from content pack" />&nbsp;</span>
              }
                <span>{this.props.description}</span>
              </div>
            }
          </Col>

          <Col sm={12} lgHidden mdHidden smHidden>
            {actionsContainer}
          </Col>
        </Row>

        <Row className="row-sm">
          {this.props.contentRow}
        </Row>
      </li>
    );
  }
}

export default EntityListItem;
