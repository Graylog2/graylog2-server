import React from 'react';
import PropTypes from 'prop-types';
import { Badge, Nav } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import Routes from 'routing/Routes';
import InactiveNavItem from './InactiveNavItem';
import badgeStyles from '../bootstrap/Badge.css';

const { NotificationsActions, NotificationsStore } = CombinedProvider.get('Notifications');

class NotificationBadge extends React.PureComponent {
  static propTypes = {
    total: PropTypes.number,
  };

  static defaultProps = {
    total: undefined,
  };

  componentDidMount() {
    NotificationsActions.list();
    this.interval = setInterval(NotificationsActions.list, this.POLL_INTERVAL);
  }

  componentWillUnmount() {
    clearInterval(this.interval);
  }

  POLL_INTERVAL = 3000;

  render() {
    const { total } = this.props;
    if (!total) {
      return null;
    }
    return (
      <Nav navbar>
        <LinkContainer to={Routes.SYSTEM.OVERVIEW}>
          <InactiveNavItem className="notification-badge-link">
            <Badge className={badgeStyles.badgeDanger} id="notification-badge">{total}</Badge>
          </InactiveNavItem>
        </LinkContainer>
      </Nav>
    );
  }
}

export default connect(NotificationBadge, { notifications: NotificationsStore }, ({ notifications }) => ({ total: notifications ? notifications.total : undefined }));
