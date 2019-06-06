import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import numeral from 'numeral';
import _ from 'lodash';
import { NavItem } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';

import { Spinner } from 'components/common';

import styles from './GlobalThroughput.css';

const GlobalThroughputStore = StoreProvider.getStore('GlobalThroughput');

const GlobalThroughput = createReactClass({
  displayName: 'GlobalThroughput',
  mixins: [Reflux.connect(GlobalThroughputStore)],

  render() {
    const { throughput } = this.state;

    return (
      <NavItem className={styles['total-throughput']} {...this.props}>
        {(_.isNil(throughput) || _.isEmpty(throughput)) === true
          ? <Spinner text="" />
          : (
            <strong className={styles['total-throughput__content']}>
              <span>{numeral(1234).format('0,0')}</span>
              <span>{numeral(1234).format('0,0')}</span>
            </strong>
          )
        }
      </NavItem>
    );
  },
});

export default GlobalThroughput;
