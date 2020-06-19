import React from 'react';
import PropTypes from 'prop-types';

import { Popover } from 'components/graylog';

class EventKeyHelpPopover extends React.Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
  };

  render() {
    const { id, ...otherProps } = this.props;
    return (
      <Popover id={id} title="More about Event Keys" {...otherProps}>
        <p>
          Event Keys are Fields used to arrange Events into groups. A group is created for each unique Key, so
          Graylog will generate as many Events as unique Keys are found. Example:
        </p>
        <p>
          <b>No Event Keys:</b> One Event for each <em>Login failure</em> message.<br />
          <b>Event Key <code>username</code>:</b> One Event for each username with a <em>Login failure</em> message.
        </p>
      </Popover>
    );
  }
}

export default EventKeyHelpPopover;
