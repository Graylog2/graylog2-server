import PropTypes from 'prop-types';
import React from 'react';

import { Badge } from 'react-bootstrap';
import { Link } from 'react-router';
import Routes from 'routing/Routes';

import ContentPackStatusStyle from './ContentPackStatus.css';

class ContentPackStatus extends React.Component {
  static propTypes = {
    states: PropTypes.arrayOf(PropTypes.string),
    contentPackId: PropTypes.string.isRequired,
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
      return (
        <Link key={state} to={Routes.SYSTEM.CONTENTPACKS.show(this.props.contentPackId)}>
          <Badge key={state} bsClass={`badge ${ContentPackStatus.styleMap[state]}`}>{state}</Badge>
        </Link>
      );
    });
    return (
      <span>
        {badges}
      </span>
    );
  }
}

export default ContentPackStatus;
