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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { get } from 'lodash';

import { AdditionalContext } from 'views/logic/ActionContext';
import { DEFAULT_HIGHLIGHT_COLOR } from 'views/Constants';

import PossiblyHighlight from './PossiblyHighlight';

type Props = {
  field: string,
  value: any,
};

const Highlight = ({ field, value }: Props) => (
  <AdditionalContext.Consumer>
    {({ message }) => (
      <PossiblyHighlight field={field}
                         color={DEFAULT_HIGHLIGHT_COLOR}
                         value={value}
                         highlightRanges={get(message, 'highlight_ranges')} />
    )}
  </AdditionalContext.Consumer>
);

Highlight.propTypes = {
  field: PropTypes.string.isRequired,
  value: PropTypes.any.isRequired,
};

export default Highlight;
