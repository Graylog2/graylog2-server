import PropTypes from 'prop-types';
import React from 'react';

class WidgetHeader extends React.Component {
  static propTypes = {
    title: PropTypes.string.isRequired,
  };

  render() {

    return (
      <div>
        <div className="widget-title">
          {this.props.title}
        </div>
        <div className="clearfix" />
      </div>
    );
  }
}

export default WidgetHeader;
