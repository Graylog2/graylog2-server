import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'react-bootstrap';
import { inject, observer } from 'mobx-react';
import moment from 'moment';
import DateTime from 'logic/datetimes/DateTime';
import { Spinner, Timestamp } from 'components/common';
import StoreProvider from 'injection/StoreProvider';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const SystemInfoStore = StoreProvider.getStore('SystemInfo');

const TimesList = React.createClass({
  propTypes: {
    currentUser: PropTypes.object.isRequired,
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
    const currentUser = this.props.currentUser;
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

export default inject(() => ({
  currentUser: CurrentUserStore.currentUser,
  isLoading: SystemInfoStore.isLoading,
  systemInfo: SystemInfoStore.systemInfo,
}))(observer(TimesList));
