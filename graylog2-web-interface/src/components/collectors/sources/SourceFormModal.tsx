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
import * as React from 'react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { FormikTouched, FormikErrors } from 'formik';
import { Formik, Form } from 'formik';
import isEqual from 'lodash/isEqual';
import moment from 'moment';
import { Trans, useTranslation } from 'react-i18next';

import i18n from 'i18n';
import { HelpBlock, Input } from 'components/bootstrap';
import Modal from 'components/bootstrap/Modal';
import { FormikInput, InputDescription } from 'components/common';
import TimeUnitInput, { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import ModalSubmit from 'components/common/ModalSubmit';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import { SOURCE_TYPE_LABELS } from './Constants';

import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type {
  Source,
  SourceType,
  FileSourceConfig,
  JournaldSourceConfig,
  JournaldPriority,
  WindowsEventLogSourceConfig,
  MacOSUnifiedLoggingSourceConfig,
} from '../types';

type Props = {
  fleetId: string;
  source?: Source;
  onClose: () => void;
  onSave: (source: Omit<Source, 'id'>) => Promise<{ id?: string } | void>;
};

const defaultConfigs: Record<
  SourceType,
  FileSourceConfig | JournaldSourceConfig | WindowsEventLogSourceConfig | MacOSUnifiedLoggingSourceConfig
> = {
  file: { paths: [''], read_mode: 'end' },
  journald: { read_mode: 'end', priority: 'info' },
  windows_event_log: { channels: [], include_default_channels: true, read_mode: 'end' },
  macos_unified_logging: {
    predicate:
      'subsystem IN {\n' +
      "'com.apple.opendirectoryd',\n" +
      "'com.apple.authorization',\n" +
      "'com.apple.loginwindow',\n" +
      "'com.apple.securityd',\n" +
      "'com.apple.TCC',\n" +
      "'com.apple.alf',\n" +
      "'com.apple.networkextension',\n" +
      "'com.apple.DiskManagement',\n" +
      "'com.apple.CoreStorage',\n" +
      "'com.apple.endpointsecurity',\n" +
      "'com.apple.syspolicyd',\n" +
      "'com.apple.launchd'\n" +
      '} AND messageType >= error',
    max_log_age: 'PT0S',
    max_poll_interval: 'PT30S',
  },
};

type FormValues = {
  source_type: SourceType;
  name: string;
  description: string;
  enabled: boolean;
  config: FileSourceConfig | JournaldSourceConfig | WindowsEventLogSourceConfig | MacOSUnifiedLoggingSourceConfig;
};

// TimeUnitInput is not a formik field, so we need to validate against the entire form
const validateMacOSUnifiedLogging = (
  config: MacOSUnifiedLoggingSourceConfig,
): FormikErrors<MacOSUnifiedLoggingSourceConfig> => {
  const errors: FormikErrors<MacOSUnifiedLoggingSourceConfig> = {};

  if (moment.duration(config.max_poll_interval).asSeconds() <= 0) {
    errors.max_poll_interval = i18n.t('source_form.max_poll_interval_error', { ns: 'collectors' });
  }

  if (moment.duration(config.max_log_age).asSeconds() < 0) {
    errors.max_log_age = i18n.t('source_form.max_log_age_error', { ns: 'collectors' });
  }

  return errors;
};

const validate = (values: FormValues) => {
  const errors: FormikErrors<FormValues> = {};

  if (!values.name) {
    errors.name = i18n.t('fleet_form.name_required', { ns: 'collectors' });
  }

  if (values.source_type === 'macos_unified_logging') {
    const configErrors = validateMacOSUnifiedLogging(values.config as MacOSUnifiedLoggingSourceConfig);

    if (Object.keys(configErrors).length > 0) {
      errors.config = configErrors;
    }
  }

  return errors;
};

export const splitToList = (value: string | undefined): Array<string> =>
  (value || '')
    .split(',')
    .map((c) => c.trim())
    .filter(Boolean);

const ReadModeOptions = () => {
  const { t } = useTranslation('collectors');

  return (
    <>
      <option value="end">{t('source_form.read_mode_end')}</option>
      <option value="beginning">{t('source_form.read_mode_beginning')}</option>
    </>
  );
};

const FileConfigFields = ({
  config,
  setFieldValue,
}: {
  config: FileSourceConfig;
  setFieldValue: (field: string, value: unknown) => void;
}) => {
  // Keep a raw input buffer so the user can freely type commas and spaces.
  // splitToList would strip a trailing comma on every keystroke otherwise,
  // making it impossible to enter multiple comma-separated values.
  const { t } = useTranslation('collectors');
  const [pathsInput, setPathsInput] = useState(() => config.paths?.join(', ') ?? '');

  return (
    <>
      <Input
        id="file-paths"
        type="text"
        label={t('source_form.file_paths')}
        help={t('source_form.file_paths_help')}
        value={pathsInput}
        onChange={(e) => {
          setPathsInput(e.target.value);
          setFieldValue('config', { ...config, paths: splitToList(e.target.value) });
        }}
        required
      />
      <Input
        id="file-read-mode"
        type="select"
        label={t('source_form.read_mode')}
        help={t('source_form.read_mode_file_help')}
        value={config.read_mode}
        onChange={(e) => setFieldValue('config', { ...config, read_mode: e.target.value as 'beginning' | 'end' })}>
        <ReadModeOptions />
      </Input>
    </>
  );
};

const JournaldConfigFields = ({
  config,
  setFieldValue,
}: {
  config: JournaldSourceConfig;
  setFieldValue: (field: string, value: unknown) => void;
}) => {
  const { t } = useTranslation('collectors');

  return (
    <>
      <Input
        id="journald-read-mode"
        type="select"
        label={t('source_form.read_mode')}
        help={t('source_form.read_mode_journald_help')}
        value={config.read_mode}
        onChange={(e) => setFieldValue('config', { ...config, read_mode: e.target.value as 'beginning' | 'end' })}>
        <ReadModeOptions />
      </Input>
      <Input
        id="journald-priority"
        type="select"
        label={t('source_form.priority')}
        help={t('source_form.priority_help')}
        value={config.priority}
        onChange={(e) => setFieldValue('config', { ...config, priority: e.target.value as JournaldPriority })}>
        <option value="emerg">{t('source_form.priority_emerg')}</option>
        <option value="alert">{t('source_form.priority_alert')}</option>
        <option value="crit">{t('source_form.priority_crit')}</option>
        <option value="err">{t('source_form.priority_err')}</option>
        <option value="warning">{t('source_form.priority_warning')}</option>
        <option value="notice">{t('source_form.priority_notice')}</option>
        <option value="info">{t('source_form.priority_info')}</option>
        <option value="debug">{t('source_form.priority_debug')}</option>
      </Input>
      <Input
        id="journald-match-pattern"
        type="text"
        label={t('source_form.match_pattern')}
        help={t('source_form.match_pattern_help')}
        value={config.match_pattern || ''}
        onChange={(e) => setFieldValue('config', { ...config, match_pattern: e.target.value || undefined })}
      />
    </>
  );
};

const WindowsEventLogConfigFields = ({
  config,
  setFieldValue,
}: {
  config: WindowsEventLogSourceConfig;
  setFieldValue: (field: string, value: unknown) => void;
}) => {
  const { t } = useTranslation('collectors');
  const [channelsInput, setChannelsInput] = useState(() => config.channels.join(', '));

  return (
    <>
      <Input
        id="win-channels"
        type="text"
        label={t('source_form.channels')}
        help={t('source_form.channels_help')}
        value={channelsInput}
        onChange={(e) => {
          setChannelsInput(e.target.value);
          setFieldValue('config', {
            ...config,
            channels: splitToList(e.target.value),
          });
        }}
        required={!config.include_default_channels}
      />
      <Input
        id="win-include-default-channels"
        type="checkbox"
        label={t('source_form.include_default_channels')}
        // if you update this, also update WindowsEventLogReceiverConfig.java
        help={t('source_form.include_default_channels_help', {
          channels:
            'Application, System, Security, Setup, Microsoft-Windows-Windows Defender/Operational, Microsoft-Windows-PowerShell/Operational, Windows PowerShell',
        })}
        checked={config.include_default_channels}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
          setFieldValue('config', { ...config, include_default_channels: e.target.checked })
        }
      />
      <Input
        id="win-read-mode"
        type="select"
        label={t('source_form.read_mode')}
        value={config.read_mode}
        onChange={(e) => setFieldValue('config', { ...config, read_mode: e.target.value as 'beginning' | 'end' })}>
        <ReadModeOptions />
      </Input>
    </>
  );
};

// Ordered largest-first so extractDurationAndUnit renders the most readable unit (e.g. PT24H -> "1 days").
const POLL_INTERVAL_UNITS = ['HOURS', 'MINUTES', 'SECONDS'];
const LOG_AGE_UNITS = ['DAYS', 'HOURS', 'MINUTES', 'SECONDS'];

const MacOSUnifiedLoggingConfigFields = ({
  config,
  setFieldValue,
  errors = undefined,
}: {
  config: MacOSUnifiedLoggingSourceConfig;
  setFieldValue: (field: string, value: unknown) => void;
  errors?: FormikErrors<MacOSUnifiedLoggingSourceConfig>;
}) => {
  const { t } = useTranslation('collectors');
  const pollInterval = extractDurationAndUnit(config.max_poll_interval, POLL_INTERVAL_UNITS);
  const logAge = extractDurationAndUnit(config.max_log_age, LOG_AGE_UNITS);

  // TimeUnitInput reports (value, unit, checked). Unchecked clears the field so the backend default applies.
  const updateDuration = (field: 'max_poll_interval' | 'max_log_age') => (value: number, unit: string) => {
    setFieldValue('config', {
      ...config,
      [field]: moment.duration(value, unit as moment.unitOfTime.DurationConstructor).toISOString(),
    });
  };

  return (
    <>
      <Input
        id="macos-predicate"
        type="textarea"
        label={t('source_form.predicate')}
        help={
          <span>
            <Trans
              t={t}
              i18nKey="source_form.predicate_help"
              values={{ example: "subsystem == 'com.apple.securityd'" }}
              components={{ code: <code /> }}
            />
          </span>
        }
        value={config.predicate || ''}
        onChange={(e) => setFieldValue('config', { ...config, predicate: e.target.value || undefined })}
      />
      <TimeUnitInput
        label={t('source_form.max_poll_interval')}
        update={updateDuration('max_poll_interval')}
        value={pollInterval.duration}
        unit={pollInterval.unit}
        units={POLL_INTERVAL_UNITS}
        required
      />
      <InputDescription error={errors?.max_poll_interval} help={t('source_form.max_poll_interval_help')} />
      <TimeUnitInput
        label={t('source_form.max_log_age')}
        update={updateDuration('max_log_age')}
        value={logAge.duration}
        unit={logAge.unit}
        units={LOG_AGE_UNITS}
        required
      />
      <InputDescription error={errors?.max_log_age} help={t('source_form.max_log_age_help')} />
    </>
  );
};

const SourceFormModal = ({ fleetId, source = undefined, onClose, onSave }: Props) => {
  const { t } = useTranslation('collectors');
  const isEdit = !!source;
  const sendTelemetry = useSendCollectorsTelemetry();

  const initialValues: FormValues = useMemo(
    () => ({
      source_type: source?.type || 'file',
      name: source?.name || '',
      description: source?.description || '',
      enabled: source?.enabled ?? true,
      config: source?.config || defaultConfigs[source?.type || 'file'],
    }),
    [source],
  );

  useEffect(() => {
    if (!isEdit) {
      sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.SOURCE.CREATE_OPENED, {
        app_action_value: 'source-create-open',
        fleet_id: fleetId,
      });
    }
    // fire only once on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Ref kept up-to-date from the Formik render prop so handleClose (which lives
  // outside Formik) can report abandonment context on cancel.
  const formStateRef = useRef<{
    dirty: boolean;
    touched: FormikTouched<FormValues>;
    values: FormValues;
  }>({ dirty: false, touched: {}, values: initialValues });

  const handleClose = useCallback(() => {
    const { dirty, touched, values } = formStateRef.current;
    const fields_touched = Object.keys(touched).filter((k) => Boolean((touched as Record<string, unknown>)[k]));

    if (isEdit) {
      // Abandoning an edit is reported against the values the form opened with, mirroring
      // SOURCE.UPDATED, so a cancelled edit and a saved one carry the same dimensions.
      sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.SOURCE.EDIT_CANCELLED, {
        app_action_value: 'source-edit-cancel',
        fleet_id: fleetId,
        source_id: source!.id,
        dirty,
        fields_touched,
        source_type: values.source_type,
        source_type_changed: initialValues.source_type !== values.source_type,
        enabled: values.enabled,
        enabled_changed: initialValues.enabled !== values.enabled,
        config_changed: !isEqual(initialValues.config, values.config),
      });
    } else {
      // On create there is nothing to compare against, so the baseline is the form's defaults.
      sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.SOURCE.CREATE_CANCELLED, {
        app_action_value: 'source-create-cancel',
        fleet_id: fleetId,
        dirty,
        fields_touched,
        source_type: values.source_type,
        source_type_changed_from_default: values.source_type !== 'file',
        enabled: values.enabled,
        enabled_toggled: values.enabled !== true,
      });
    }

    onClose();
  }, [fleetId, isEdit, source, initialValues, onClose, sendTelemetry]);

  const handleSubmit = useCallback(
    (values: FormValues) =>
      onSave({
        fleet_id: fleetId,
        name: values.name,
        description: values.description,
        enabled: values.enabled,
        type: values.source_type,
        config: values.config,
      } as Omit<Source, 'id'>).then((saved) => {
        if (isEdit) {
          sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.SOURCE.UPDATED, {
            app_action_value: 'source-edit-submit',
            fleet_id: fleetId,
            source_id: source!.id,
            source_type: values.source_type,
            enabled: values.enabled,
            enabled_changed: initialValues.enabled !== values.enabled,
            config_changed: !isEqual(initialValues.config, values.config),
          });
        } else {
          const savedId = (saved as { id?: string } | undefined)?.id ?? '';
          sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.SOURCE.CREATED, {
            app_action_value: 'source-create-submit',
            fleet_id: fleetId,
            source_id: savedId,
            source_type: values.source_type,
            enabled: values.enabled,
          });
        }

        onClose();
      }),
    [fleetId, isEdit, onSave, onClose, sendTelemetry, source, initialValues],
  );

  return (
    <Modal show onHide={handleClose} bsSize="lg">
      <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} validate={validate}>
        {({ isSubmitting, isValidating, isValid, values, setFieldValue, dirty, touched, errors }) => {
          formStateRef.current = { dirty, touched, values };

          return (
            <Form>
              <Modal.Header>
                <Modal.Title>{isEdit ? t('source_form.edit_title') : t('source_form.new_title')}</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <p>{t('source_form.description')}</p>
                <Input
                  id="source-type"
                  type="select"
                  label={t('source_form.source_type')}
                  value={values.source_type}
                  onChange={(e) => {
                    const newType = e.target.value as SourceType;

                    setFieldValue('source_type', newType);
                    setFieldValue('config', defaultConfigs[newType]);
                  }}
                  disabled={isEdit}>
                  {Object.entries(SOURCE_TYPE_LABELS).map(([value, labelKey]) => (
                    <option key={value} value={value}>
                      {t(labelKey)}
                    </option>
                  ))}
                </Input>
                {values.source_type === 'file' && <HelpBlock>{t('source_form.file_type_help')}</HelpBlock>}
                {values.source_type === 'journald' && <HelpBlock>{t('source_form.journald_type_help')}</HelpBlock>}
                {values.source_type === 'windows_event_log' && (
                  <HelpBlock>{t('source_form.windows_type_help')}</HelpBlock>
                )}
                {values.source_type === 'macos_unified_logging' && (
                  <HelpBlock>
                    <Trans t={t} i18nKey="source_form.macos_type_help" components={{ code: <code /> }} />
                  </HelpBlock>
                )}
                <FormikInput id="source-name" label={t('fleet_form.name')} name="name" required />
                <FormikInput
                  id="source-description"
                  label={t('fleet_form.description')}
                  name="description"
                  type="textarea"
                />
                <FormikInput
                  id="source-enabled"
                  label={t('source_columns.enabled')}
                  name="enabled"
                  type="checkbox"
                  help={t('source_form.enabled_help')}
                />
                {values.source_type === 'file' && (
                  <FileConfigFields config={values.config as FileSourceConfig} setFieldValue={setFieldValue} />
                )}
                {values.source_type === 'journald' && (
                  <JournaldConfigFields config={values.config as JournaldSourceConfig} setFieldValue={setFieldValue} />
                )}
                {values.source_type === 'windows_event_log' && (
                  <WindowsEventLogConfigFields
                    config={values.config as WindowsEventLogSourceConfig}
                    setFieldValue={setFieldValue}
                  />
                )}
                {values.source_type === 'macos_unified_logging' && (
                  <MacOSUnifiedLoggingConfigFields
                    config={values.config as MacOSUnifiedLoggingSourceConfig}
                    setFieldValue={setFieldValue}
                    errors={errors.config as FormikErrors<MacOSUnifiedLoggingSourceConfig>}
                  />
                )}
              </Modal.Body>
              <Modal.Footer>
                <ModalSubmit
                  submitButtonText={isEdit ? t('source_form.update') : t('source_form.create')}
                  submitLoadingText={isEdit ? t('fleet_form.updating') : t('fleet_form.creating')}
                  onCancel={handleClose}
                  disabledSubmit={isValidating || !isValid}
                  isSubmitting={isSubmitting}
                />
              </Modal.Footer>
            </Form>
          );
        }}
      </Formik>
    </Modal>
  );
};

export default SourceFormModal;
