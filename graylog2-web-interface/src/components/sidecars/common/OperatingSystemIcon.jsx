import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

const OperatingSystemIcon = createReactClass({
  propTypes: {
    operatingSystem: PropTypes.string,
  },

  getDefaultProps() {
    return {
      operatingSystem: undefined,
    };
  },


  operatingSystemIcon(operatingSystem) {
    let glyphClass = 'fa-question-circle';
    if (operatingSystem) {
      const os = operatingSystem.trim().toLowerCase();
      if (os.indexOf('darwin') !== -1 || os.indexOf('mac os') !== -1) {
        glyphClass = 'fa-apple';
      } else if (os.indexOf('linux') !== -1) {
        glyphClass = 'fa-linux';
      } else if (os.indexOf('win') !== -1) {
        glyphClass = 'fa-windows';
      }
    }

    glyphClass += ' fa-fw sidecar-os';

    return (<i className={`fa ${glyphClass}`} />);
  },

  render() {
    return this.operatingSystemIcon(this.props.operatingSystem);
  },
});

export default OperatingSystemIcon;
