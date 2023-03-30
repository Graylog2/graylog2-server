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
import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { library } from '@fortawesome/fontawesome-svg-core';
import { fas } from '@fortawesome/free-solid-svg-icons';
import { far } from '@fortawesome/free-regular-svg-icons';
import { faApple, faFreebsd, faGithub, faGithubAlt, faLinux, faWindows } from '@fortawesome/free-brands-svg-icons';

library.add(fas, far, faApple, faGithub, faGithubAlt, faLinux, faWindows, faFreebsd);

const CustomFontAwesomeIcon = (props: React.ComponentProps<typeof FontAwesomeIcon>) => <FontAwesomeIcon {...props} />;

export default CustomFontAwesomeIcon;
