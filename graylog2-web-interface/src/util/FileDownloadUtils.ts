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

import UserNotification from 'util/UserNotification';
import { fetchBlobFile, fetchFile } from 'logic/rest/FetchProvider';

export const createLinkAndDownload = (href: string, fileName: string) => {
  const a = document.createElement('a');
  a.href = href;
  a.download = fileName;

  if (document.body) {
    document.body.appendChild(a);
  }

  a.click();
  a.remove();
};

export const downloadBLOB = (contents: BlobPart, metadata: { fileName: string, contentType: string }) => {
  // create blob from contents and meta data
  const blob = new Blob([contents], { type: metadata.contentType });

  // eslint-disable-next-line compat/compat
  const href = window.URL.createObjectURL(blob);
  createLinkAndDownload(href, metadata.fileName);
};

const errorHandler = (errorThrown: Error) => {
  UserNotification.error(`Downloading failed with status: ${errorThrown}`, 'Unable to download');
};

export const fetchTextFile = async <Body>(method: string, url: string, body: Body | undefined, mimeType: string, fileName: string) => fetchFile(method, url, body, mimeType)
  .then((result: string) => downloadBLOB(result, { fileName, contentType: mimeType }))
  .catch(errorHandler);

export const fetchBinaryFile = async <Body>(method: string, url: string, body: Body | undefined, mimeType: string, fileName: string) => fetchBlobFile(method, url, body, mimeType)
  .then((result: Blob) => downloadBLOB(result, { fileName, contentType: mimeType }))
  .catch(errorHandler);
