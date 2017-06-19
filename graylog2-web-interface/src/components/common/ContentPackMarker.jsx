import React from 'react';

const ContentPackMarker = React.createClass({
  propTypes: {
    contentPack: React.PropTypes.string,
    marginLeft: React.PropTypes.number,
    marginRight: React.PropTypes.number,
  },

  getDefaultProps() {
    return { contentPack: undefined, marginLeft: 0, marginRight: 0 };
  },

  render() {
    const style = { marginLeft: this.props.marginLeft, marginRight: this.props.marginRight };

    if (this.props.contentPack) {
      return <i className="fa fa-cube" title="Created from content pack" style={style} />;
    }

    return null;
  },
});

export default ContentPackMarker;
