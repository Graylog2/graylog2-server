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
import React from 'react';
import PropTypes from 'prop-types';

import useFeature from 'hooks/useFeature';

type Props = {
  name: string;
  fallback?: React.ReactNode,
  children: React.ReactNode,
}

const Feature = ({ name, fallback, children }: Props) => {
  const hasFeature = useFeature(name);

  if (hasFeature) {
    return children;
  }

  return fallback;
};

Feature.propTypes = {
  name: PropTypes.string.isRequired,
  fallback: PropTypes.node,
  children: PropTypes.node.isRequired,
};

Feature.defaultProps = {
  fallback: null,
};

export default Feature;
