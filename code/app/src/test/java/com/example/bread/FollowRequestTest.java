package com.example.bread;

import com.example.bread.model.FollowRequest;
import com.google.firebase.Timestamp;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class FollowRequestTest {

    private FollowRequest request;

    @Before
    public void setUp() {
        request = new FollowRequest("john_doe");
    }

    @Test
    public void testConstructor_InitializesFieldsCorrectly() {
        assertEquals("john_doe", request.getFromUsername());
        assertEquals("pending", request.getStatus());
        assertEquals(FollowRequest.RequestStatus.PENDING, request.getStatusEnum());
        assertNotNull(request.getTimestamp());
    }

    @Test
    public void testSetAndGetFromUsername() {
        request.setFromUsername("alice");
        assertEquals("alice", request.getFromUsername());
    }

    @Test
    public void testSetAndGetStatusWithString() {
        request.setStatus("accepted");
        assertEquals("accepted", request.getStatus());
        assertEquals(FollowRequest.RequestStatus.ACCEPTED, request.getStatusEnum());
    }

    @Test
    public void testSetAndGetStatusWithEnum() {
        request.setStatusEnum(FollowRequest.RequestStatus.DECLINED);
        assertEquals("declined", request.getStatus());
        assertEquals(FollowRequest.RequestStatus.DECLINED, request.getStatusEnum());
    }

    @Test
    public void testGetStatusEnumFallbackToPending() {
        request.setStatus("unknown_status");
        assertEquals(FollowRequest.RequestStatus.PENDING, request.getStatusEnum());
    }

    @Test
    public void testSetAndGetTimestamp() {
        Timestamp now = Timestamp.now();
        request.setTimestamp(now);
        assertEquals(now, request.getTimestamp());
    }

    @Test
    public void testToMapContainsCorrectValues() {
        Timestamp now = Timestamp.now();
        request.setTimestamp(now);
        request.setStatus("accepted");
        request.setFromUsername("bob");

        Map<String, Object> map = request.toMap();
        assertEquals("bob", map.get("fromUsername"));
        assertEquals("accepted", map.get("status"));
        assertEquals(now, map.get("timestamp"));
    }

    @Test
    public void testRequestStatus_fromString() {
        assertEquals(FollowRequest.RequestStatus.PENDING, FollowRequest.RequestStatus.fromString("pending"));
        assertEquals(FollowRequest.RequestStatus.ACCEPTED, FollowRequest.RequestStatus.fromString("accepted"));
        assertEquals(FollowRequest.RequestStatus.DECLINED, FollowRequest.RequestStatus.fromString("declined"));
        assertEquals(FollowRequest.RequestStatus.PENDING, FollowRequest.RequestStatus.fromString("invalid"));
    }

    @Test
    public void testRequestStatus_getValue() {
        assertEquals("pending", FollowRequest.RequestStatus.PENDING.getValue());
        assertEquals("accepted", FollowRequest.RequestStatus.ACCEPTED.getValue());
        assertEquals("declined", FollowRequest.RequestStatus.DECLINED.getValue());
    }
}
