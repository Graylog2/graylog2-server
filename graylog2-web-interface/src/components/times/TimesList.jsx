import React from 'react';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';
import moment from 'moment';
import DateTime from 'logic/datetimes/DateTime';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const SystemStore = StoreProvider.getStore('System');

import { Spinner, Timestamp } from 'components/common';

const TimesList = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore), Reflux.connect(SystemStore)],
  getInitialState() {
    return { time: moment() };
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
    const timeFormat = DateTime.Formats.DATETIME_TZ;
    const currentUser = this.state.currentUser;
    const serverTimezone = this.state.system.timezone;
    return (
      <Row className="content">
        <Col md={12}>
          <h2>Time configuration</h2>

          <p className="description">
            Dealing with timezones can be confusing. Here you can see the timezone applied to different components of your system.
            You can check timezone settings of specific graylog-server nodes on their respective detail page.
          </p>

          <dl className="system-dl">
            <dt>User <em>{currentUser.username}</em>:</dt>
            <dd><Timestamp dateTime={time} format={timeFormat} /></dd>
            <dt>Your web browser:</dt>
            <dd><Timestamp dateTime={time} format={timeFormat} tz={'browser'} /></dd>
            <dt>Graylog server:</dt>
            <dd><Timestamp dateTime={time} format={timeFormat} tz={serverTimezone} /></dd>
          </dl>
        </Col>
      </Row>
    );
  },
});

export default TimesList;
