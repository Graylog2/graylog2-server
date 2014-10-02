/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
