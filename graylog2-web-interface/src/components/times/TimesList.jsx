import React from 'react';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';
import moment from 'moment';
import momentHelper from 'legacy/moment-helper';

import CurrentUserStore from 'stores/users/CurrentUserStore';
import SystemStore from 'stores/system/SystemStore';

import { Spinner } from 'components/common';

const TimesList = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore), Reflux.connect(SystemStore)],
  getInitialState() {
    return {time: moment()};
  },
  componentDidMount() {
    this.interval = setInterval(() => this.setState(this.getInitialState()), 1000);
  },
  componentWillUnmount() {
    clearInterval(this.interval);
  },
  render() {
    if (!this.state.system) {
      return <Spinner />;
    }
    const time = this.state.time;
    const timeFormat = momentHelper.DATE_FORMAT_TZ;
    const currentUser = this.state.currentUser;
    const serverTimezone = this.state.system.timezone;
    return (
      <Row className="content">
        <Col md={12}>
          <h2><i className="fa fa-clock-o"/> Time configuration</h2>

          <p className="description">
            Dealing with timezones can be confusing. Here you can see the timezone applied to different components of your system.
            You can check timezone settings of specific graylog-server nodes on their respective detail page.
          </p>

          <dl className="system-time">
            <dt>User <em>{currentUser.username}</em></dt>
            <dd><time>{time.clone().utc().tz(currentUser.timezone).format(timeFormat)}</time></dd>
            <dt>Your web browser:</dt>
            <dd><time>{time.clone().format(timeFormat)}</time></dd>
            <dt>Graylog server:</dt>
            <dd><time>{time.clone().utc().tz(serverTimezone).format(timeFormat)}</time></dd>
          </dl>
        </Col>
      </Row>
    );
  },
});

export default TimesList;
