package com.smartestate.service;

import com.smartestate.model.Channel;
import com.smartestate.model.DeliveryLog;
import com.smartestate.model.DeliveryStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregated delivery analytics for a notification (FR-2.2). */
public class DeliveryStats {

    public int recipients;
    public int attempts;
    public int delivered;   // DELIVERED or READ
    public int read;
    public int failed;
    public int pending;     // SENT or PENDING
    public int readReceiptCapable; // attempts on channels that support read receipts

    public Map<Channel, int[]> perChannel = new LinkedHashMap<>(); // [attempts, delivered, read, failed]

    public int deliveryRatePct() {
        return attempts == 0 ? 0 : Math.round(delivered * 100f / attempts);
    }

    public int readRatePct() {
        return readReceiptCapable == 0 ? 0 : Math.round(read * 100f / readReceiptCapable);
    }

    public static DeliveryStats of(List<DeliveryLog> logs) {
        DeliveryStats s = new DeliveryStats();
        java.util.Set<Long> recips = new java.util.HashSet<>();
        for (DeliveryLog l : logs) {
            s.attempts++;
            if (l.getRecipient() != null) recips.add(l.getRecipient().getId());
            Channel ch = l.getChannel();
            s.perChannel.putIfAbsent(ch, new int[4]);
            int[] c = s.perChannel.get(ch);
            c[0]++;
            DeliveryStatus st = l.getStatus();
            if (ch.isSupportsReadReceipt()) s.readReceiptCapable++;
            switch (st) {
                case DELIVERED -> { s.delivered++; c[1]++; }
                case READ -> { s.delivered++; s.read++; c[1]++; c[2]++; }
                case FAILED -> { s.failed++; c[3]++; }
                case SENT, PENDING -> s.pending++;
            }
        }
        s.recipients = recips.size();
        return s;
    }
}
