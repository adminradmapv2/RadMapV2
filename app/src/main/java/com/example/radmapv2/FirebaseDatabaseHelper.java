package com.example.radmapv2;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FirebaseDatabaseHelper {
    private FirebaseDatabase mDatabase;
    private DatabaseReference mReferenceAntenas;
    private List<Antena> Antenas = new ArrayList<>();

    public interface DataStatus{
        void DataIsLoaded(Map<String, Antena> antenas);
        void DataIsInserted();
        void DataIsUpdated();
        void DataIsDeleted();
    }

    public FirebaseDatabaseHelper(){
        mDatabase = FirebaseDatabase.getInstance();
        mReferenceAntenas = mDatabase.getReference("Antenas");
    }

    public void readAntenas(final DataStatus dataStatus){
        mReferenceAntenas.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Antenas.clear();

                Map<String, Antena> antenaMap = StreamSupport
                        .stream(snapshot.getChildren().spliterator(), true)
                        .collect(Collectors.toMap(DataSnapshot::getKey, value -> value.getValue(Antena.class)));
                dataStatus.DataIsLoaded(antenaMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
