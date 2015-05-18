'use strict';

var StringUtils = {
    capitalizeFirstLetter(text: string): string {
        return text.charAt(0).toUpperCase() + text.slice(1);
    }
};

export = StringUtils;