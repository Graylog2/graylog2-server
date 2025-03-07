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

export const DIAGNOSIS_HELP = {
  INPUT_LISTENING_ON:
    'Note that bind address needs to direct to the Graylog service. Note that the selected port needs to be free of port conflict with another input on each node. Remember that a local input on a single node might conflict with a global input that is runs on all nodes, causing the input to fail on a single input',
  INPUT_LISTENING_FOR:
    'Note that UPD traffic sent to a TCP input, or TCP traffic sent to a UDP input will show up as network I/O, but no messages will be received.',
  EMPTY_MESSAGES_DISCARDED: 'describe the criteria for a mesage to be discarded as empty.',
  NETWORK_IO: 'Note that this value can be used to detect connection attempts and invalid traffic to an input.',
  INPUT_STATE:
    'An Input in running State is ready to receive messages. An Input in a failed or failing state has encountered a problem; click on the button below to view the associated error message.',
  MESSAGE_ERROR_AT_INPUT:
    'Each input type expects to receive messages in a particular format. Messages that break this format can be rejected by the Input, in which case they will not proceed to processing. To resolve these cases, review the messages that are being sent. More descriptive errors can be found in Graylog’s server.log file.',
  MESSAGE_FAILED_TO_PROCESS:
    'Within Graylog, extractors and pipelines can be used to perform processing operations on messages such as parsing out fields. A pipeline rule that tries to perform a nonsensical operation, or that modifies the properties of a field in the message to a format that conflicts with the data type or field length limits of that field, can fail to process. Messages that fail to process in this fashion can be reviewed within the Processing and Indexing Failures stream.',
  MESSAGE_FAILED_TO_INDEX:
    'Within the search cluster, a message can only be saved into an Index if it meets the schema of that Index. Each field in Opensearch has a field type and character limit; if either is exceeded, the message will fail to Index. Messages that fail to Index in this fashion can be reviewed within the Processing and Indexing Failures stream.',
  RECEIVED_MESSAGE_COUNT_BY_STREAM:
    'Use this view to understand where stream and pipeline rules are directing the messages from this Input. A message can be duplicated into multiple streams; if this results in the message being saved to more than one Index, this can also multiply license usage. This view can be useful to detect such duplication.',
};

export default DIAGNOSIS_HELP;
