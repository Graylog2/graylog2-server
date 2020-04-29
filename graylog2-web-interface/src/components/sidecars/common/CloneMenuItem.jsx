import React, { forwardRef } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { util } from 'theme';
import { MenuItem } from 'components/graylog';
import { BootstrapModalForm, Input } from 'components/bootstrap';

const StyledMenuItem = styled(MenuItem)(({ theme }) => `
  /**
  * Bootstrap styling breaks here, since the component needs to be wrapped around another element.
  * This style replicates Bootstrap's making the menu item look normal.
  */
  a {
    display: block;
    padding: 3px 20px;
    clear: both;
    font-weight: normal;
    line-height: 1.42857143;
    color: ${theme.color.global.textDefault};
    white-space: nowrap;

    &:hover,
    &:focus {
      text-decoration: none;
      color: ${util.contrastingColor(theme.color.gray[90])};
      background-color: ${theme.color.gray[90]};
    }
  }
`);

const CloneMenuItem = forwardRef(({ onSelect, onSave, id, onChange, error, name }, ref) => {
  return (
    <span>
      <StyledMenuItem onSelect={onSelect}>Clone</StyledMenuItem>
      <BootstrapModalForm ref={ref}
                          title="Clone"
                          onSubmitForm={onSave}
                          submitButtonDisabled={!!error}
                          submitButtonText="Done">
        <fieldset>
          <Input type="text"
                 id={id}
                 label="Name"
                 defaultValue={name}
                 onChange={onChange}
                 bsStyle={error ? 'error' : null}
                 help={error || 'Type a name for the new collector'}
                 autoFocus
                 required />
        </fieldset>
      </BootstrapModalForm>
    </span>
  );
});

CloneMenuItem.propTypes = {
  onSelect: PropTypes.func.isRequired,
  onSave: PropTypes.func.isRequired,
  id: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  error: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
};

export default CloneMenuItem;
