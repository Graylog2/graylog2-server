// @flow strict
import * as React from 'react';

import styles from './HighlightingRules.css';

type Props = {
  color: string,
};

const ColorBox = ({ color }: Props) => <div className={styles.colorElement} style={{ backgroundColor: color }} />;

export default ColorBox;
