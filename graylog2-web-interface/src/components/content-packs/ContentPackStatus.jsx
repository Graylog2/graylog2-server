import PropTypes from 'prop-types';
import React from 'react';

import { Badge } from 'react-bootstrap';

import ContentPackStatusStyle from './ContentPackStatus.css';

class ContentPackStatus extends React.Component {
  static propTypes = {
    states: PropTypes.arrayOf(PropTypes.string),
  };

  static defaultProps = {
    states: [],
  };

  static styleMap = {
    installed: ContentPackStatusStyle.installed,
    updatable: ContentPackStatusStyle.updatable,
    edited: ContentPackStatusStyle.edited,
    error: ContentPackStatusStyle.error,
  };

  render() {
    const badges = this.props.states.map((state) => {
      return (<Badge key={state} bsClass={`badge ${ContentPackStatus.styleMap[state]}`}>{state}</Badge>);
    });
    return (
      <div>
        {badges}
      </div>
    );
  }
}

export default ContentPackStatus;
