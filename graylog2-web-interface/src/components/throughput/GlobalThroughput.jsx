import React from 'react';

import { useStore } from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import NumberUtils from 'util/NumberUtils';
import { NavItem } from 'components/graylog';
import { Spinner } from 'components/common';

import styles from './GlobalThroughput.css';

const GlobalThroughputStore = StoreProvider.getStore('GlobalThroughput');

const GlobalThroughput = (props) => {
  const { throughput } = useStore(GlobalThroughputStore);
  let output = <Spinner text="" />;

  if (!throughput.loading) {
    const inputNumeral = NumberUtils.formatNumber(throughput.input);
    const outputNumeral = NumberUtils.formatNumber(throughput.output);

    output = (
      <strong className={styles['total-throughput__content']}
              aria-label={`In ${inputNumeral} / Out ${outputNumeral} msg/s`}>
        <span className={`${styles['total-throughput__data']} ${styles['total-throughput__data--in']}`}>
          <span>{inputNumeral}</span> <i>in</i>
        </span>
        <span className={styles['total-throughput__data']}>
          <span>{outputNumeral}</span> <i>out</i>
        </span>
      </strong>
    );
  }

  return (
    <NavItem className={styles['total-throughput']} {...props}>
      {output}
    </NavItem>
  );
};

export default GlobalThroughput;
