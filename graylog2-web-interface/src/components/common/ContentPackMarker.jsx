import PropTypes from 'prop-types';
import React from 'react';

/**
 * Adds an icon to an entity that was created by a content pack.
 */
const ContentPackMarker = React.createClass({
  propTypes: {
    /** Content pack key of the entity's object. When set, the component will render the content pack marker. */
    contentPack: PropTypes.string,
    /** Margin-left the marker should use. */
    marginLeft: PropTypes.number,
    /** Margin-right the marker should use. */
    marginRight: PropTypes.number,
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
