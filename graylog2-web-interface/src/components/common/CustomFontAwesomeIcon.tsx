import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { library } from '@fortawesome/fontawesome-svg-core';
import { fas } from '@fortawesome/free-solid-svg-icons';
import { far } from '@fortawesome/free-regular-svg-icons';
import { faApple, faGithub, faGithubAlt, faLinux, faWindows } from '@fortawesome/free-brands-svg-icons';

library.add(fas, far, faApple, faGithub, faGithubAlt, faLinux, faWindows);

const CustomFontAwesomeIcon = (props: React.ComponentProps<typeof FontAwesomeIcon>) => <FontAwesomeIcon {...props} />;

export default CustomFontAwesomeIcon;
