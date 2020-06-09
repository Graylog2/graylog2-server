import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

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
      color: ${theme.utils.contrastingColor(theme.color.gray[90])};
      background-color: ${theme.color.gray[90]};
    }
  }
`);

const CloneMenuItem = ({ error, id, modalRef, name, onChange, onSave, onSelect }) => {
  return (
    <span>
      <StyledMenuItem onSelect={onSelect}>Clone</StyledMenuItem>
      <BootstrapModalForm ref={modalRef}
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
};

CloneMenuItem.propTypes = {
  error: PropTypes.string,
  id: PropTypes.string.isRequired,
  modalRef: PropTypes.shape({ current: PropTypes.any }).isRequired,
  name: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  onSave: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
};

CloneMenuItem.defaultProps = {
  error: undefined,
};

export default CloneMenuItem;
