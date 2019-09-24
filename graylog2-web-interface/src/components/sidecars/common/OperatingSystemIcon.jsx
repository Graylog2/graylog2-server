import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

import { Icon } from 'components/common';
import commonStyles from 'components/sidecars/common/CommonSidecarStyles.css';

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
    let glyphName = 'question-circle';
    if (operatingSystem) {
      const os = operatingSystem.trim().toLowerCase();
      if (os.indexOf('darwin') !== -1 || os.indexOf('mac os') !== -1) {
        glyphName = 'apple';
      } else if (os.indexOf('linux') !== -1) {
        glyphName = 'linux';
      } else if (os.indexOf('win') !== -1) {
        glyphName = 'windows';
      }
    }

    return (<Icon name={glyphName} className={commonStyles.sidecarOs} fixedWidth />);
  },

  render() {
    return this.operatingSystemIcon(this.props.operatingSystem);
  },
});

export default OperatingSystemIcon;
