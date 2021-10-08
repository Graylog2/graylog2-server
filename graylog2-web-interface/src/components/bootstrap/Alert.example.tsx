/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';

import { ColorVariants } from 'theme/colors';
import { Icon } from 'components/common';

import Alert from './Alert';

// eslint-disable-next-line import/prefer-default-export
export const DefaultExample = () => {
  // ### Default
  return (
    <>
      <Alert>
        <Icon name="star" fixedWidth bsSize="lg" />{' '}
        <strong>default</strong> Lorem ipsum dolor sit amet consectetur <a href="#lorem">adipisicing elit</a>.
      </Alert>
    </>
  );
};

export const VariantExamples = () => {
  // ### Variants

  const styles: Array<ColorVariants> = ['danger', 'info', 'success', 'warning'];

  return (
    <>
      {styles.map((style) => {
        return (
          <Alert bsStyle={style} key={`button-${style}`}>
            <Icon name="exclamation-triangle" fixedWidth bsSize="lg" />{' '}
            <strong>{style}</strong> Lorem ipsum dolor sit amet consectetur <a href="#lorem">adipisicing elit</a>.
          </Alert>
        );
      })}
    </>
  );
};
