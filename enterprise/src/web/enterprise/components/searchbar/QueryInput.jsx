import React from 'react';
import PropTypes from 'prop-types';

export default function QueryInput({ value, onChange }) {
  return (
    <div className="query">
      <Input type="text"
             name="q"
             value={value}
             onChange={event => onChange(event.target.value)}
             placeholder="Type your search query here and press enter. (&quot;not found&quot; AND http) OR http_response_code:[400 TO 404]" />
    </div>
  );
};

QueryInput.propTypes = {
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
};
