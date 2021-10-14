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

import { Icon } from 'components/common';

import Alert from './Alert';

export const DefaultExample = () => {
  return (
    <>
      <Alert>
        <Icon name="star" fixedWidth bsSize="lg" />{' '}
        <strong>default</strong> Lorem ipsum dolor sit amet consectetur <a href="#lorem">adipisicing elit</a>.
      </Alert>
    </>
  );
};

export const VariantsExample = () => {
  return (
    <>
      <Alert bsStyle="danger">
        <Icon name="bomb" fixedWidth bsSize="lg" />{' '}
        <strong>danger</strong> Lorem ipsum dolor sit amet consectetur <a href="#lorem">adipisicing elit</a>.
      </Alert>

      <Alert bsStyle="info">
        <Icon name="info-circle" fixedWidth bsSize="lg" />{' '}
        <strong>info</strong> Lorem ipsum dolor sit amet consectetur <a href="#lorem">adipisicing elit</a>.
      </Alert>

      <Alert bsStyle="success">
        <Icon name="check-circle" fixedWidth bsSize="lg" />{' '}
        <strong>success</strong> Lorem ipsum dolor sit amet consectetur <a href="#lorem">adipisicing elit</a>.
      </Alert>

      <Alert bsStyle="warning">
        <Icon name="exclamation-triangle" fixedWidth bsSize="lg" />{' '}
        <strong>warning</strong> Lorem ipsum dolor sit amet consectetur <a href="#lorem">adipisicing elit</a>.
      </Alert>
    </>
  );
};
