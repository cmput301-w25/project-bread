package com.example.bread;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.example.bread.model.Comment;
import com.google.firebase.firestore.DocumentReference;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

public class CommentTest {

    private DocumentReference mockParticipantRef;

    @Before
    public void setUp() {
        mockParticipantRef = mock(DocumentReference.class);
    }

    @Test
    public void testConstructor_SetsIdTextAndParticipantRef() {
        Comment comment = new Comment(mockParticipantRef, "Hello world!");

        assertNotNull(comment.getId());
        assertEquals("Hello world!", comment.getText());
        assertEquals(mockParticipantRef, comment.getParticipantRef());
    }

    @Test
    public void testSetAndGetId() {
        Comment comment = new Comment();
        String id = UUID.randomUUID().toString();
        comment.setId(id);

        assertEquals(id, comment.getId());
    }

    @Test
    public void testSetAndGetText() {
        Comment comment = new Comment();
        comment.setText("Sample comment");

        assertEquals("Sample comment", comment.getText());
    }

    @Test
    public void testSetAndGetTimestamp() {
        Comment comment = new Comment();
        Date now = new Date();
        comment.setTimestamp(now);

        assertEquals(now, comment.getTimestamp());
    }

    @Test
    public void testSetAndGetParticipantRef() {
        Comment comment = new Comment();
        comment.setParticipantRef(mockParticipantRef);

        assertEquals(mockParticipantRef, comment.getParticipantRef());
    }

    @Test
    public void testCompareTo_BothTimestampsNull() {
        Comment c1 = new Comment();
        Comment c2 = new Comment();

        assertEquals(0, c1.compareTo(c2));
    }

    @Test
    public void testCompareTo_ThisTimestampNull() {
        Comment c1 = new Comment();
        Comment c2 = new Comment();
        c2.setTimestamp(new Date());

        assertTrue(c1.compareTo(c2) < 0);
    }

    @Test
    public void testCompareTo_OtherTimestampNull() {
        Comment c1 = new Comment();
        Comment c2 = new Comment();
        c1.setTimestamp(new Date());

        assertTrue(c1.compareTo(c2) > 0);
    }

    @Test
    public void testCompareTo_BothTimestampsSet() throws InterruptedException {
        Comment c1 = new Comment();
        Comment c2 = new Comment();

        Date earlier = new Date();
        Thread.sleep(10); // ensure different timestamps
        Date later = new Date();

        c1.setTimestamp(earlier);
        c2.setTimestamp(later);

        assertTrue(c1.compareTo(c2) < 0);
        assertTrue(c2.compareTo(c1) > 0);
        assertEquals(0, c1.compareTo(c1));
    }

    @Test
    public void testToString_ReturnsExpectedFormat() {
        Comment comment = new Comment(mockParticipantRef, "Test comment");
        comment.setId("1234");
        Date now = new Date();
        comment.setTimestamp(now);

        String result = comment.toString();
        assertTrue(result.contains("1234"));
        assertTrue(result.contains("Test comment"));
        assertTrue(result.contains(now.toString()));
    }
}
