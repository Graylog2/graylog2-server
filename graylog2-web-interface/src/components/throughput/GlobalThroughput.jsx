import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import numeral from 'numeral';
import { NavItem } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';

import { Spinner } from 'components/common';

import styles from './GlobalThroughput.css';

const GlobalThroughputStore = StoreProvider.getStore('GlobalThroughput');

const GlobalThroughput = createReactClass({
  displayName: 'GlobalThroughput',
  mixins: [Reflux.connect(GlobalThroughputStore)],

  getInitialState() {
    return { throughput: { loading: true } };
  },

  render() {
    const { throughput } = this.state;
    let output = <Spinner text="" />;

    if (!throughput.loading) {
      const inputNumeral = numeral(throughput.input).format('0,0');
      const outputNumeral = numeral(throughput.output).format('0,0');

      output = (
        <strong className={styles['total-throughput__content']}
                aria-label={`In ${inputNumeral} / Out ${outputNumeral} msg/s`}>
          <span>{inputNumeral} <i>in</i></span>
          <span>{outputNumeral} <i>out</i></span>
        </strong>
      );
    }


    return (
      <NavItem className={styles['total-throughput']} {...this.props}>
        {output}
      </NavItem>
    );
  },
});

export default GlobalThroughput;
