import React from 'react';
import PropTypes from 'prop-types';
import { Badge, Nav } from 'components/graylog';
import { LinkContainer } from 'react-router-bootstrap';
import styled from 'styled-components';

import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import Routes from 'routing/Routes';
import InactiveNavItem from './InactiveNavItem';

const { NotificationsActions, NotificationsStore } = CombinedProvider.get('Notifications');

const StyledInactiveNavItem = styled(InactiveNavItem)`
  a:hover {
    border: 0;
    text-decoration: none;
  }
`;

class NotificationBadge extends React.PureComponent {
  POLL_INTERVAL = 3000;

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

  render() {
    const { total } = this.props;
    if (!total) {
      return null;
    }
    return (
      <Nav navbar>
        <LinkContainer to={Routes.SYSTEM.OVERVIEW}>
          <StyledInactiveNavItem>
            <Badge bsStyle="danger" id="notification-badge">{total}</Badge>
          </StyledInactiveNavItem>
        </LinkContainer>
      </Nav>
    );
  }
}

export default connect(NotificationBadge, { notifications: NotificationsStore }, ({ notifications }) => ({ total: notifications ? notifications.total : undefined }));
