package com.example.radmapv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.os.Bundle;
import android.os.LocaleList;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

//public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
public class MainActivity extends AppCompatActivity {

    FusedLocationProviderClient client;                                //Usuario APP
    SupportMapFragment mapFragment;                                    //Fragment para el Mapa
    private ClusterManager<MyItem> mClusterManager;                    //Cluster para marcadores
    private List<WeightedLatLng> latLngs = new ArrayList<>();         //Lista para latitudes y longitudes del HeatMap
    private List<PositionToIntensity> intensity = new ArrayList<>();   //Lista para latitudes y longitudes del HeatMap
    Map<String, Antena> AntenasG = new HashMap<>();                    //Mapa de antenas

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Se obtiene el fragment donde se ubicara el mapa
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        //Inicializar ubicación actual
        client = LocationServices.getFusedLocationProviderClient(this);

        //Se verifican permisos
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Se llama el metodo que obtiene la ubicación actual y llena los clustes de marcadores
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return;
        }
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                if (location != null) {
                    mapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {

                            //Implementación de la clase FirebaseDatabaseHelper que nos ayuda a obtener los datos de la DB
                            new FirebaseDatabaseHelper().readAntenas(new FirebaseDatabaseHelper.DataStatus() {
                                @Override
                                public void DataIsLoaded(Map<String, Antena> antenas) {
                                    //Cuando se cargan los datos completamente de la DB a la APP se mueve la camara a la ubicación del usuario
                                    //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(4.7103, -74.0322), 17));
                                    //Se llena la variable global con los datos de las antenas
                                    AntenasG = antenas;
                                    if (AntenasG != null) {
                                        AntenasG.entrySet().forEach(antenaEntry -> {
                                            double random_double = Math.random() * (100 - 1 + 1) + 1;
                                            intensity.add(new PositionToIntensity(antenaEntry.getValue().getLat(), antenaEntry.getValue().getLon(), random_double));
                                        });
                                    }
                                }

                                @Override
                                public void DataIsInserted() {

                                }

                                @Override
                                public void DataIsUpdated() {

                                }

                                @Override
                                public void DataIsDeleted() {

                                }
                            });

                            //Metodo que escucha cuando la ubicación del Mapa cambia
                            googleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                                @Override
                                public void onCameraIdle() {
                                    //Se obtiene los datos de la regison visible del mapa en la pantalla del telefono
                                    LatLngBounds bounds = googleMap.getProjection().getVisibleRegion().latLngBounds;

                                    //Metodo para llenar el cluster de marcadores con los datos de las antenas
                                    setUpCluster(googleMap, AntenasG, location, bounds);
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void setUpCluster(GoogleMap googleMap, Map<String, Antena> antenas, Location location, LatLngBounds bounds) {
//        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 7));

        //Se inicializa el cluster con el contexto del mapa
        mClusterManager = new ClusterManager<MyItem>(this, googleMap);
        ClusterRenderer clusterRenderer = new ClusterRenderer(this, googleMap, mClusterManager);

        //Se borran los datos del mapa
        mClusterManager.clearItems();
        latLngs.clear();
        googleMap.clear();

        //googleMap.setOnCameraIdleListener(mClusterManager);
        googleMap.setOnMarkerClickListener(mClusterManager);
        mClusterManager.setAnimation(true);

        //Añade los items al cluster de marcadores
        addItems(antenas, bounds);

        // Create a heat map tile provider, passing it the latlngs of the police stations.
        if (latLngs.size() != 0) {
            HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                    .weightedData(latLngs)
                    //.radius(50)
                    .build();

            provider.setRadius(150);

            // Add a tile overlay to the map, using the heat map tile provider.
            googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
        }
    }

    private void addItems(Map<String, Antena> antenas, LatLngBounds bounds) {
        //Obtiene latitud y longitud del area visible del telefono
        LatLng northeast = bounds.northeast;
        Double boundLat = northeast.latitude;
        Double boundLong = northeast.longitude;

        LatLng southwest = bounds.southwest;
        Double boundLat2 = southwest.latitude;
        Double boundLong2 = southwest.longitude;

        //Llena cluster solo con los datos del area visible del telefono
        antenas.entrySet().stream().map(Map.Entry::getValue)
                .distinct()
                .filter(antena -> checkAntenaInBounds(antena, boundLat, boundLong, boundLat2, boundLong2))
                .forEach(antena -> {
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(new LatLng(antena.getLat(), antena.getLon()))
                            .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_antenna));
                    MyItem markerItem = new MyItem(markerOptions);
                    //MyItem offsetItem = new MyItem(antena.getLat(), antena.getLon());
                    mClusterManager.addItem(markerItem);

                    intensity.stream()
                            .filter(ints -> antena.getLat().equals(ints.getLat()))
                            .filter(ints -> antena.getLon().equals(ints.getLon()))
                            .forEach(ints -> {
                                latLngs.add(new WeightedLatLng(new LatLng(antena.getLat(), antena.getLon()), ints.getIntens()));
                            });

                });
        mClusterManager.cluster();
    }

    private boolean checkAntenaInBounds(Antena antena, Double boundLatN, Double boundLongN, Double boundLatS, Double boundLongS) {
        return Double.valueOf(boundLatN) >= antena.getLat() &&
                Double.valueOf(boundLatS) <= antena.getLat() &&
                Double.valueOf(boundLongN) >= antena.getLon() &&
                Double.valueOf(boundLongS) <= antena.getLon();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 44) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int VectorResID) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, VectorResID);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}