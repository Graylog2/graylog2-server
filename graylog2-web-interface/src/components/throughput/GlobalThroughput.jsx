import React, { useEffect, useState } from 'react';
import numeral from 'numeral';

import { useStore } from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';

import { NavItem } from 'components/graylog';
import { Spinner } from 'components/common';

import styles from './GlobalThroughput.css';

const GlobalThroughputStore = StoreProvider.getStore('GlobalThroughput');

const GlobalThroughput = (props) => {
  // const [throughput, setThroughput] = useState({ throughput: { loading: true } });
  const throughput = useStore(GlobalThroughputStore);


  // useEffect(() => {
  //   return () => {
  //     clearInterval(interval);
  //   };
  // }, []);

  let output = <Spinner text="" />;

  if (!throughput.loading) {
    const inputNumeral = numeral(throughput.input).format('0,0');
    const outputNumeral = numeral(throughput.output).format('0,0');

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
