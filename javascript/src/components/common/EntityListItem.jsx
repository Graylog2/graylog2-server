import React, {PropTypes} from 'react';

const EntityListItem = React.createClass({
  propTypes: {
    title: PropTypes.string.isRequired,
    titleSuffix: PropTypes.any,
    description: PropTypes.any,
    actions: PropTypes.array,
    createdFromContentPack: PropTypes.bool,
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
      <li className="stream">
        <h2>{this.props.title} {titleSuffix}</h2>

        <div className="stream-data">
          <div className="stream-actions pull-right">
            {this.props.actions}
          </div>
          <div className="stream-description">
            {this.props.createdFromContentPack && <i className="fa fa-cube" title="Created from content pack"/>}
            <span>{this.props.description}</span>
          </div>
        </div>
      </li>
    );
  },
});

export default EntityListItem;
