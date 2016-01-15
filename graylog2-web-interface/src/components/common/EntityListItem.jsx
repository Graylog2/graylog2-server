import React, {PropTypes} from 'react';
import {Row, Col} from 'react-bootstrap';

const EntityListItem = React.createClass({
  propTypes: {
    title: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
    titleSuffix: PropTypes.any,
    description: PropTypes.any,
    actions: PropTypes.oneOfType([PropTypes.array, PropTypes.node]),
    createdFromContentPack: PropTypes.bool,
    contentRow: PropTypes.node,
  },
  getDefaultProps() {
    return {
      createdFromContentPack: false,
    };
  },
  render() {
    let titleSuffix;
    if (this.props.titleSuffix) {
      titleSuffix = <small>{this.props.titleSuffix}</small>;
    }
    return (
      <li className="entity-list-item">
        <Row className="row-sm">
          <Col md={6}>
            <h2>{this.props.title} {titleSuffix}</h2>
            <div className="item-description">
              {this.props.createdFromContentPack && <i className="fa fa-cube" title="Created from content pack"/>}
              <span>{this.props.description}</span>
            </div>
          </Col>

          <Col md={6}>
            <div className="item-actions pull-right">
              {this.props.actions}
            </div>
          </Col>
        </Row>

        <Row className="row-sm">
          {this.props.contentRow}
        </Row>
      </li>
    );
  },
});

export default EntityListItem;
