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
import PropTypes from 'prop-types';
import React, { useState } from 'react';

import { Button, Input } from 'components/bootstrap';
import { optionalMarker } from 'components/configurationforms/FieldHelpers';

import type { EncryptedFieldValue, InlineBinaryField as InlineBinaryFieldType } from './types';

type Props = {
  autoFocus: boolean,
  field: InlineBinaryFieldType,
  dirty: boolean,
  onChange: (title: string, value: any) => void,
  title: string,
  typeName: string,
  value?: string | EncryptedFieldValue<string>,
};

const InlineBinaryField = ({ field, title, typeName, dirty, onChange, value, autoFocus }: Props) => {
  const [fileName, setFileName] = useState(undefined);
  const isEncryptedValuePresent = typeof value !== 'string' && value.is_set;
  const isRequired = !field.is_optional;
  const showReadOnlyEncrypted = field.is_encrypted && !dirty && isEncryptedValuePresent;
  const fieldId = `${typeName}-${title}`;

  const labelContent = <>{field.human_name} {optionalMarker(field)}</>;
  const helpText = fileName ? `Currently using file: ${fileName}` : field.description;

  const handleFileRead = (fileReader: FileReader, file) => {
    const dataUrl = fileReader.result;

    if (dataUrl && typeof dataUrl === 'string') {
      const dataString = dataUrl.replace(/^data:[\s\S]+\/[\s\S]+;base64,/, '');
      setFileName(file.name);

      if (field.is_encrypted) {
        onChange(title, { set_value: dataString });
      } else {
        onChange(title, dataString);
      }
    }
  };

  const encryptedButtonAfter = () => {
    if (isEncryptedValuePresent) {
      return (
        <Button type="button" onClick={() => onChange(title, { delete_value: true })}>
          Reset
        </Button>
      );
    }

    return null;
  };

  const buttonAfter = () => {
    if ((fileName && fileName !== '') || (typeof value === 'string' && value.length > 0)) {
      return (
        <Button type="button" onClick={() => { setFileName(undefined); onChange(title, ''); }}>
          Remove
        </Button>
      );
    }

    return null;
  };

  const handleFileUpload = (file: File) => {
    const fileReader = new FileReader();

    fileReader.onloadend = (_) => handleFileRead(fileReader, file);

    if (file) {
      fileReader.readAsDataURL(file);
    }
  };

  return (
    showReadOnlyEncrypted ? (
      <Input id={fieldId}
             type="password"
             name={`configuration[${title}]`}
             label={labelContent}
             required={isRequired}
             readOnly
             help={helpText}
             value="encrypted value"
             buttonAfter={encryptedButtonAfter()}
             autoFocus={autoFocus} />
    ) : (
      <Input id={fieldId}
             type="file"
             name={`configuration[${title}]`}
             label={labelContent}
             required={isRequired}
             help={helpText}
             onChange={(e) => handleFileUpload(e.target.files[0])}
             buttonAfter={buttonAfter()}
             autoFocus={autoFocus} />
    ));
};

InlineBinaryField.propTypes = {
  autoFocus: PropTypes.bool,
  dirty: PropTypes.bool,
  field: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  typeName: PropTypes.string.isRequired,
  value: PropTypes.string,
};

InlineBinaryField.defaultProps = {
  autoFocus: false,
  dirty: false,
  value: '',
};

export default InlineBinaryField;
