import React from 'react';

import { Input } from 'components/bootstrap';

const DSVHTTPAdapterFieldSet = ({ handleFormEvent, validationState, validationMessage, config}) => {
  return (<fieldset>
    <Input type="text"
           id="url"
           name="url"
           label="File URL"
           autoFocus
           required
           onChange={handleFormEvent}
           help={validationMessage('url', 'The URL of the DSV file.')}
           bsStyle={validationState('url')}
           value={config.url}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
    <Input type="number"
           id="refresh_interval"
           name="refresh_interval"
           label="Refresh interval"
           required
           onChange={handleFormEvent}
           help="The interval to check if the DSV file needs a reload. (in seconds)"
           value={config.refresh_interval}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
    <Input type="text"
           id="separator"
           name="separator"
           label="Separator"
           required
           onChange={handleFormEvent}
           help="The delimiter to use for separating entries."
           value={config.separator}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
    <Input type="text"
           id="quotechar"
           name="quotechar"
           label="Quote character"
           required
           onChange={handleFormEvent}
           help="The character to use for quoted elements."
           value={config.quotechar}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
    <Input type="text"
           id="ignorechar"
           name="ignorechar"
           label="Ignore characters"
           required
           onChange={handleFormEvent}
           help="Ignore lines starting with these characters."
           value={config.ignorechar}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
    <Input type="text"
           id="key_column"
           name="key_column"
           label="Key column"
           required
           onChange={handleFormEvent}
           help="The column name that should be used for the key lookup."
           value={config.key_column}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
    <Input type="text"
           id="value_column"
           name="value_column"
           label="Value column"
           required
           onChange={handleFormEvent}
           help="The column name that should be used as the value for a key."
           value={config.value_column}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
    <Input type="checkbox"
           id="case_insensitive_lookup"
           name="case_insensitive_lookup"
           label="Allow case-insensitive lookups"
           checked={config.case_insensitive_lookup}
           onChange={handleFormEvent}
           help="Enable if the key lookup should be case-insensitive."
           wrapperClassName="col-md-offset-3 col-md-9" />
    <Input type="checkbox"
           id="check_presence_only"
           name="check_presence_only"
           label="Check Presence Only"
           checked={config.check_presence_only}
           onChange={handleFormEvent}
           help="Only check if key is present in table, returns boolean instead of value."
           wrapperClassName="col-md-offset-3 col-md-9" />
  </fieldset>);
};

export default DSVHTTPAdapterFieldSet;
