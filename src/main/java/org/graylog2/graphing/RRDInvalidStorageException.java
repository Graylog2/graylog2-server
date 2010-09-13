/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2.graphing;

/**
 * RRDInvalidStorageException.java: Sep 11, 2010 9:05:44 PM
 *
 * Thrown in case of invalid RRD storage folder. (i.e. Does not exist / Not a directory)
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class RRDInvalidStorageException extends Exception {

    /**
     *
     */
    public RRDInvalidStorageException(){
    }

    /**
     *
     * @param msg
     */
    public RRDInvalidStorageException(String msg){
        super(msg);
    }
    
}