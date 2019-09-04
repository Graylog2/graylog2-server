import React from 'react';
import PropTypes from 'prop-types';

import { Button, Icon } from 'components/graylog';

const SearchButton = ({ running, disabled, glyph }) => (
  <Button type="submit" bsStyle={running ? 'warning' : 'success'} disabled={disabled} className="pull-left search-button-execute">
    <Icon className={running ? 'fa fa-spinner fa-pulse fa-fw' : `fa fa-${glyph}`} />
  </Button>
);

SearchButton.defaultProps = {
  running: false,
  disabled: false,
  glyph: 'search',
};

SearchButton.propTypes = {
  running: PropTypes.bool,
  disabled: PropTypes.bool,
  glyph: PropTypes.string,
};

export default SearchButton;
