import React from 'react';
import { PropTypes } from 'prop-types';
import Reflux from 'reflux';
import { connect } from 'react-redux';
import { Col, Row } from 'react-bootstrap';
import moment from 'moment';
import DateTime from 'logic/datetimes/DateTime';

import { Spinner, Timestamp } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import { loadSystemInfo } from 'ducks/system';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const TimesList = React.createClass({
  propTypes: {
    isLoading: PropTypes.bool,
    system: PropTypes.object,
    loadSystemInfo: PropTypes.func.isRequired,
  },

  mixins: [Reflux.connect(CurrentUserStore)],
  getInitialState() {
    return { time: moment() };
  },
  componentWillMount() {
    this.props.loadSystemInfo();
  },
  componentDidMount() {
    this.interval = setInterval(() => this.setState(this.getInitialState()), 1000);
  },
  componentWillUnmount() {
    clearInterval(this.interval);
  },
  render() {
    const { isLoading, system } = this.props;
    if (isLoading) {
      return <Spinner />;
    }
    const time = this.state.time;
    const timeFormat = DateTime.Formats.DATETIME_TZ;
    const currentUser = this.state.currentUser;
    const serverTimezone = system.timezone;
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

const mapStateToProps = (state) => {
  return {
    system: state.entities.systemInfo,
    isLoading: state.frontend.isLoading,
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    loadSystemInfo: () => dispatch(loadSystemInfo()),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(TimesList);
