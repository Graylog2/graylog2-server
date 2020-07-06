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
