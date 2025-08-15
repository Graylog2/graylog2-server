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

import useCurrentUser from 'hooks/useCurrentUser';
import { useStore } from 'stores/connect';
import { Button } from 'components/bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import UrlAllowListForm from 'components/configurations/UrlAllowListForm';
import type { ConfigurationsStoreState, AllowListConfig } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
// Explicit import to fix eslint import/no-cycle
import IfPermitted from 'components/common/IfPermitted';
import { isPermitted } from 'util/PermissionsMixin';
import generateId from 'logic/generateId';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const URL_ALLOWLIST_CONFIG = 'org.graylog2.system.urlallowlist.UrlAllowlist';

type Props = {
  newUrlEntry?: string;
  onUpdate?: () => void;
  urlType?: 'regex' | 'literal';
};

const URLAllowListFormModal = ({ newUrlEntry = '', urlType = undefined, onUpdate = () => {} }: Props) => {
  const prevNewUrlEntry = useRef<string>();
  const [config, setConfig] = useState<AllowListConfig>({ entries: [], disabled: false });
  const [isValid, setIsValid] = useState<boolean>(false);
  const [newUrlEntryId, setNewUrlEntryId] = useState<string | undefined>();
  const [showConfigModal, setShowConfigModal] = useState<boolean>(false);

  const { configuration } = useStore<ConfigurationsStoreState>(ConfigurationsStore);
  const urlAllowListConfig = configuration[URL_ALLOWLIST_CONFIG];

  const currentUser = useCurrentUser();
  const sendTelemetry = useSendTelemetry();

  useEffect(() => {
    if (isPermitted(currentUser.permissions, ['urlallowlist:read'])) {
      ConfigurationsActions.listAllowListConfig(URL_ALLOWLIST_CONFIG);
    }
  }, [currentUser]);

  const setDefaultAllowListState = useCallback(
    (defaultUrlAllowListConfig) => {
      const id = generateId();
      const defaultConfig = {
        entries: [
          ...defaultUrlAllowListConfig.entries,
          {
            id: id,
            title: '',
            value: newUrlEntry,
            type: urlType ?? 'literal',
          },
        ],
        disabled: defaultUrlAllowListConfig.disabled,
      };
      setNewUrlEntryId(id);
      setConfig(defaultConfig);
    },
    [newUrlEntry, urlType],
  );

  useEffect(() => {
    const { entries } = config;

    if (urlAllowListConfig) {
      if (entries.length === 0 || prevNewUrlEntry.current !== newUrlEntry) {
        setDefaultAllowListState(urlAllowListConfig);
      }
    }

    prevNewUrlEntry.current = newUrlEntry;
  }, [setDefaultAllowListState, urlAllowListConfig, config, newUrlEntry, urlType]);

  const openModal = () => {
    setShowConfigModal(true);
  };

  const closeModal = () => {
    setShowConfigModal(false);
    setDefaultAllowListState(urlAllowListConfig);
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

    sendTelemetry(TELEMETRY_EVENT_TYPE.URLALLOWLIST_CONFIGURATION_UPDATED, {
      app_section: 'urlallowlist',
      app_action_value: 'configuration-update',
    });

    if (isValid) {
      ConfigurationsActions.updateAllowlist(URL_ALLOWLIST_CONFIG, config).then(() => {
        onUpdate();
        closeModal();
      });
    }
  };

  if (urlAllowListConfig) {
    const { entries, disabled } = config;

    return (
      <>
        <IfPermitted permissions="urlallowlist:write">
          <Button bsStyle="info" bsSize="xs" onClick={openModal}>
            Add to URL allowlist
          </Button>
        </IfPermitted>
        <BootstrapModalForm
          show={showConfigModal}
          bsSize="lg"
          title="Update Allowlist Configuration"
          onCancel={closeModal}
          onSubmitForm={saveConfig}
          submitButtonDisabled={!isValid}
          submitButtonText="Update configuration">
          <h3>Allowlist URLs</h3>
          <UrlAllowListForm
            key={newUrlEntryId}
            urls={entries}
            disabled={disabled}
            onUpdate={handleUpdate}
            newEntryId={newUrlEntryId}
          />
        </BootstrapModalForm>
      </>
    );
  }

  return null;
};

export default URLAllowListFormModal;
