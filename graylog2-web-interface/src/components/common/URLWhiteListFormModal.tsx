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
import React, { useCallback, useEffect, useState, useRef } from 'react';
import PropTypes from 'prop-types';

import useCurrentUser from 'hooks/useCurrentUser';
import { useStore } from 'stores/connect';
import { Button } from 'components/bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import UrlWhiteListForm from 'components/configurations/UrlWhiteListForm';
import type { ConfigurationsStoreState, WhiteListConfig } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
// Explicit import to fix eslint import/no-cycle
import IfPermitted from 'components/common/IfPermitted';
import { isPermitted } from 'util/PermissionsMixin';
import generateId from 'logic/generateId';

const URL_WHITELIST_CONFIG = 'org.graylog2.system.urlwhitelist.UrlWhitelist';

type Props = {
  newUrlEntry: string,
  onUpdate: () => void,
  urlType?: 'regex' | 'literal',
};

const URLWhiteListFormModal = ({ newUrlEntry, urlType, onUpdate }: Props) => {
  const configModal = useRef<BootstrapModalForm>();
  const prevNewUrlEntry = useRef<string>();
  const [config, setConfig] = useState<WhiteListConfig>({ entries: [], disabled: false });
  const [isValid, setIsValid] = useState<boolean>(false);
  const [newUrlEntryId, setNewUrlEntryId] = useState<string | undefined>();

  const { configuration } = useStore<ConfigurationsStoreState>(ConfigurationsStore);
  const urlWhiteListConfig = configuration[URL_WHITELIST_CONFIG];

  const currentUser = useCurrentUser();

  useEffect(() => {
    if (isPermitted(currentUser.permissions, ['urlwhitelist:read'])) {
      ConfigurationsActions.listWhiteListConfig(URL_WHITELIST_CONFIG);
    }
  }, [currentUser]);

  const setDefaultWhiteListState = useCallback((defaultUrlWhiteListConfig) => {
    const id = generateId();
    const defaultConfig = {
      entries: [...defaultUrlWhiteListConfig.entries, {
        id: id,
        title: '',
        value: newUrlEntry,
        type: urlType ?? 'literal',
      }],
      disabled: defaultUrlWhiteListConfig.disabled,
    };
    setNewUrlEntryId(id);
    setConfig(defaultConfig);
  }, [newUrlEntry, urlType]);

  useEffect(() => {
    const { entries } = config;

    if (urlWhiteListConfig) {
      if (entries.length === 0 || prevNewUrlEntry.current !== newUrlEntry) {
        setDefaultWhiteListState(urlWhiteListConfig);
      }
    }

    prevNewUrlEntry.current = newUrlEntry;
  }, [setDefaultWhiteListState, urlWhiteListConfig, config, newUrlEntry, urlType]);

  const openModal = () => {
    configModal.current?.open();
  };

  const closeModal = () => {
    configModal.current?.close();
  };

  const handleUpdate = (nextConfig, nextIsValid) => {
    setConfig(nextConfig);
    setIsValid(nextIsValid);
  };

  const saveConfig = (event) => {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }

    if (isValid) {
      ConfigurationsActions.updateWhitelist(URL_WHITELIST_CONFIG, config).then(() => {
        onUpdate();
        closeModal();
      });
    }
  };

  const resetConfig = () => {
    setDefaultWhiteListState(urlWhiteListConfig);
  };

  if (urlWhiteListConfig) {
    const { entries, disabled } = config;

    return (
      <>
        <IfPermitted permissions="urlwhitelist:write">
          <Button bsStyle="info" bsSize="xs" onClick={openModal}>Add to URL Whitelist</Button>
        </IfPermitted>
        <BootstrapModalForm ref={configModal}
                            bsSize="lg"
                            title="Update Whitelist Configuration"
                            onSubmitForm={saveConfig}
                            onModalClose={resetConfig}
                            submitButtonDisabled={!isValid}
                            submitButtonText="Save">
          <h3>Whitelist URLs</h3>
          <UrlWhiteListForm key={newUrlEntryId}
                            urls={entries}
                            disabled={disabled}
                            onUpdate={handleUpdate}
                            newEntryId={newUrlEntryId} />
        </BootstrapModalForm>
      </>
    );
  }

  return null;
};

URLWhiteListFormModal.propTypes = {
  newUrlEntry: PropTypes.string,
  onUpdate: PropTypes.func,
  urlType: PropTypes.oneOf(['regex', 'literal']),
};

URLWhiteListFormModal.defaultProps = {
  newUrlEntry: '',
  onUpdate: () => {},
  urlType: undefined,
};

export default URLWhiteListFormModal;
