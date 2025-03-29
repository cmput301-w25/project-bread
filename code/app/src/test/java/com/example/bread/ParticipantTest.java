package com.example.bread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.bread.model.FollowRequest;
import com.example.bread.model.Participant;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ParticipantTest {

    private Participant participant;

    @Before
    public void setUp() {
        participant = new Participant("john_doe", "john@example.com", "john", "doe");
    }

    @Test
    public void testDefaultConstructor() {
        Participant p = new Participant();
        assertNotNull(p.getFollowers());
        assertNotNull(p.getFollowing());
        assertNotNull(p.getFollowRequests());
        assertEquals(0, p.getFollowerCount());
        assertEquals(0, p.getFollowingCount());
    }

    @Test
    public void testParameterizedConstructor_CapitalizesNames() {
        assertEquals("John", participant.getFirstName());
        assertEquals("Doe", participant.getLastName());
    }

    @Test
    public void testToStringContainsKeyDetails() {
        String result = participant.toString();
        assertTrue(result.contains("username='john_doe'"));
        assertTrue(result.contains("email='john@example.com'"));
        assertTrue(result.contains("firstName='John'"));
        assertTrue(result.contains("lastName='Doe'"));
    }

    @Test
    public void testGetDisplayName() {
        assertEquals("John Doe", participant.getDisplayName());
    }

    @Test
    public void testSetAndGetFollowers() {
        participant.setFollowers(Arrays.asList("alice", "bob"));
        assertEquals(2, participant.getFollowerCount());
        assertEquals(Arrays.asList("alice", "bob"), participant.getFollowers());
    }

    @Test
    public void testSetFollowersWithNullResetsCount() {
        participant.setFollowers(null);
        assertEquals(0, participant.getFollowerCount());
        assertNull(participant.getFollowers());
    }

    @Test
    public void testSetAndGetFollowing() {
        participant.setFollowing(Collections.singletonList("charlie"));
        assertEquals(1, participant.getFollowingCount());
        assertEquals(Collections.singletonList("charlie"), participant.getFollowing());
    }

    @Test
    public void testSetFollowingWithNullResetsCount() {
        participant.setFollowing(null);
        assertEquals(0, participant.getFollowingCount());
        assertNull(participant.getFollowing());
    }

    @Test
    public void testSettersAndGetters() {
        participant.setUsername("new_user");
        participant.setEmail("new@example.com");
        participant.setFirstName("newFirst");
        participant.setLastName("newLast");
        participant.setProfilePicture("http://example.com/pic.jpg");
        participant.setFollowerCount(10);
        participant.setFollowingCount(5);

        assertEquals("new_user", participant.getUsername());
        assertEquals("new@example.com", participant.getEmail());
        assertEquals("newFirst", participant.getFirstName());
        assertEquals("newLast", participant.getLastName());
        assertEquals("http://example.com/pic.jpg", participant.getProfilePicture());
        assertEquals(10, participant.getFollowerCount());
        assertEquals(5, participant.getFollowingCount());
    }

    @Test
    public void testSetAndGetFollowRequests() {
        FollowRequest request = new FollowRequest(); // Assuming default constructor
        participant.setFollowRequests(Collections.singletonList(request));
        assertEquals(1, participant.getFollowRequests().size());
    }
}
