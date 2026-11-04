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
export const CATEGORIES = ['wrong', 'untranslated', 'awkward', 'layout', 'other'] as const;
export type Category = (typeof CATEGORIES)[number];

export const STATUSES = ['open', 'fixed', 'wont_fix'] as const;
export type Status = (typeof STATUSES)[number];

export type TranslationFeedback = {
  id: string;
  language: string;
  namespace?: string;
  key?: string;
  text: string;
  english_text?: string;
  suggestion?: string;
  comment?: string;
  category: Category;
  path?: string;
  dom_path?: string;
  reporter: string;
  status: Status;
  created_at: string;
  updated_at: string;
};

export type CreateTranslationFeedback = Omit<
  TranslationFeedback,
  'id' | 'reporter' | 'status' | 'created_at' | 'updated_at'
>;
