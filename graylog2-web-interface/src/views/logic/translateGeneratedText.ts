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
import i18n from 'i18n';

/**
 * Texts which the frontend generates once and stores in the view, e.g. the titles and axis labels of the widgets of
 * a new search. They are stored in English, so they are translated when they are shown. A text the user changed
 * isn't in this list and stays as it is.
 */
const GENERATED_TEXTS: { [text: string]: string } = {
  'Message Count': 'generated_texts.message_count',
  'All Messages': 'generated_texts.all_messages',
  'Untitled Aggregation': 'generated_texts.untitled_aggregation',
  'Untitled Message Table': 'generated_texts.untitled_message_table',
  'Untitled Events Overview': 'generated_texts.untitled_events_overview',
  'Untitled Text Widget': 'generated_texts.untitled_text_widget',
  Count: 'generated_texts.count',
  'Time Range': 'generated_texts.time_range',
};

const translateGeneratedText = <T extends string | null | undefined>(text: T): T | string => {
  const key = text ? GENERATED_TEXTS[text] : undefined;

  return key ? i18n.t(key, { ns: 'widgets' }) : text;
};

export default translateGeneratedText;
