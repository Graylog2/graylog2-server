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

package org.graylog2;

import java.util.*;
import java.io.*;

/**
 * FileWatcher.java: February 09, 2011
 *
 * Abstract utility class for watching file changes.
 *
 * @author: Joshua Spaulding <joshua.spaulding@gmail.com>
 */
public abstract class FileWatcher extends TimerTask {
  private long timeStamp;
  private File file;

  public FileWatcher( File file ) {
      this.file = file;
      this.timeStamp = file.lastModified();
  }

  public final void run() {
      long timeStamp = file.lastModified();

      if( this.timeStamp != timeStamp ) {
	      this.timeStamp = timeStamp;
	      onChange(file);
      }
  }

  protected abstract void onChange( File file );
}
