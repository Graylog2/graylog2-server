import React from 'react';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';
import { inject, observer } from 'mobx-react';
import moment from 'moment';
import DateTime from 'logic/datetimes/DateTime';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import { Spinner, Timestamp } from 'components/common';

const TimesList = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  propTypes: {
    isLoading: PropTypes.bool.isRequired,
    systemInfo: PropTypes.object,
  },

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
    const { isLoading, systemInfo } = this.props;
    if (isLoading) {
      return <Spinner />;
    }
    const time = this.state.time;
    const timeFormat = DateTime.Formats.DATETIME_TZ;
    const currentUser = this.state.currentUser;
    const serverTimezone = systemInfo.timezone;
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

export default inject(context => ({
  isLoading: context.rootStore.systemInfoStore.isLoading,
  systemInfo: context.rootStore.systemInfoStore.systemInfo,
}))(observer(TimesList));
