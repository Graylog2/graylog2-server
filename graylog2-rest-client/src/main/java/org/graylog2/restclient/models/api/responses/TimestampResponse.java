/**
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
 */
package org.graylog2.restclient.models.api.responses;

import org.joda.time.DateTime;

/**
 * Created with IntelliJ IDEA.
 * User: dennis
 * Date: 01.11.13
 * Time: 18:52
 * To change this template use File | Settings | File Templates.
 */
public class TimestampResponse {
    public Integer year;
    public Integer dayOfMonth;
    public Integer era;
    public Integer dayOfYear;
    public Integer dayOfWeek;
    public Integer monthOfYear;
    public Integer yearOfEra;
    public Integer weekOfWeekyear;
    public Integer weekyear;
    public Integer yearOfCentury;
    public Integer centuryOfEra;
    public Integer millisOfSecond;
    public Integer millisOfDay;
    public Integer secondOfMinute;
    public Integer secondOfDay;
    public Integer minuteOfHour;
    public Integer minuteOfDay;
    public Integer hourOfDay;
}
