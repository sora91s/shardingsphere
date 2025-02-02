/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.db.protocol.mysql.packet.command.query.binary.execute.protocol;

import org.apache.shardingsphere.db.protocol.mysql.payload.MySQLPacketPayload;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * Binary protocol value for date for MySQL.
 */
public final class MySQLDateBinaryProtocolValue implements MySQLBinaryProtocolValue {
    
    @Override
    public Object read(final MySQLPacketPayload payload) throws SQLException {
        int length = payload.readInt1();
        switch (length) {
            case 0:
                throw new SQLFeatureNotSupportedException("Can not support date format if year, month, day is absent.");
            case 4:
                return getTimestampForDate(payload);
            case 7:
                return getTimestampForDatetime(payload);
            case 11:
                Timestamp result = getTimestampForDatetime(payload);
                result.setNanos(payload.readInt4() * 1000);
                return result;
            default:
                throw new IllegalArgumentException(String.format("Wrong length `%d` of MYSQL_TYPE_TIME", length));
        }
    }
    
    @SuppressWarnings("MagicConstant")
    private Timestamp getTimestampForDate(final MySQLPacketPayload payload) {
        Calendar result = Calendar.getInstance();
        result.set(payload.readInt2(), payload.readInt1() - 1, payload.readInt1());
        return new Timestamp(result.getTimeInMillis());
    }
    
    @SuppressWarnings("MagicConstant")
    private Timestamp getTimestampForDatetime(final MySQLPacketPayload payload) {
        Calendar result = Calendar.getInstance();
        result.set(payload.readInt2(), payload.readInt1() - 1, payload.readInt1(), payload.readInt1(), payload.readInt1(), payload.readInt1());
        return new Timestamp(result.getTimeInMillis());
    }
    
    @Override
    public void write(final MySQLPacketPayload payload, final Object value) {
        Timestamp timestamp = new Timestamp(((Date) value).getTime());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(timestamp);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int nanos = timestamp.getNanos();
        boolean isTimeAbsent = 0 == hourOfDay && 0 == minutes && 0 == seconds;
        boolean isNanosAbsent = 0 == nanos;
        if (isTimeAbsent && isNanosAbsent) {
            payload.writeInt1(4);
            writeDate(payload, year, month, dayOfMonth);
            return;
        }
        if (isNanosAbsent) {
            payload.writeInt1(7);
            writeDate(payload, year, month, dayOfMonth);
            writeTime(payload, hourOfDay, minutes, seconds);
            return;
        }
        payload.writeInt1(11);
        writeDate(payload, year, month, dayOfMonth);
        writeTime(payload, hourOfDay, minutes, seconds);
        writeNanos(payload, nanos);
    }
    
    private void writeDate(final MySQLPacketPayload payload, final int year, final int month, final int dayOfMonth) {
        payload.writeInt2(year);
        payload.writeInt1(month);
        payload.writeInt1(dayOfMonth);
    }
    
    private void writeTime(final MySQLPacketPayload payload, final int hourOfDay, final int minutes, final int seconds) {
        payload.writeInt1(hourOfDay);
        payload.writeInt1(minutes);
        payload.writeInt1(seconds);
    }
    
    private void writeNanos(final MySQLPacketPayload payload, final int nanos) {
        payload.writeInt4(nanos);
    }
}
