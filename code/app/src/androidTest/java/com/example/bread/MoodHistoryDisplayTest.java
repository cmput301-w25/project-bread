package com.example.bread;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.androidcicd.movie.Movie;
import com.example.androidcicd.movie.MovieProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

public class MoodHistoryDisplayTest {
    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockMovieCollection;

    //mocking references to documents in movie collection
    @Mock
    private DocumentReference mockDocRef;

    private MovieProvider movieProvider;

    @Before //defining behavior of the mocked objects
    public void setUp() {

    }

    @Test
    public void testAddMovieSetsId() {

    }
}
