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
import useServerText from 'util/serverText';
import type { IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';

const NAMESPACES = ['index_set_templates'];

/** Translates the English title and description of built-in templates. Custom templates keep the text of their author. */
const useTemplateText = () => {
  const serverText = useServerText(NAMESPACES);

  return (template: Pick<IndexSetTemplate, 'built_in'>, text: string | undefined) =>
    template.built_in ? serverText(text) : text;
};

export default useTemplateText;
