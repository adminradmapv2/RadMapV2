package com.example.radmapv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.heatmaps.WeightedLatLng;

//public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
public class MainActivity extends AppCompatActivity {

    FusedLocationProviderClient client;                                //Usuario APP
    SupportMapFragment mapFragment;                                    //Fragment para el Mapa
    private ClusterManager<MyItem> mClusterManager;                    //Cluster para marcadores
    private List<WeightedLatLng> latLngs = new ArrayList<>();          //Lista para latitudes y longitudes del HeatMap
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
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(4.7103, -74.0322), 19));
                                    //Se llena la variable global con los datos de las antenas
                                    AntenasG = antenas;
                                    AtomicReference<Character> cardp = new AtomicReference<>((char) 0);
                                    if (AntenasG != null) {
                                        AntenasG.entrySet().forEach(antenaEntry -> {
                                            double random_double = Math.random() * ((25 - 5) + 5) + 5;
                                            if (random_double <= 10){
                                               cardp.set('N');
                                            }else {
                                                if (random_double <= 15){
                                                    cardp.set('S');
                                                }else {
                                                    if (random_double <= 20){
                                                        cardp.set('E');
                                                    }else {
                                                        cardp.set('O');
                                                    }
                                                }
                                            }

                                            //double random_double = ThreadLocalRandom.current().nextDouble(0.0005, 0.0010);
                                            intensity.add(new PositionToIntensity(antenaEntry.getValue().getLat(), antenaEntry.getValue().getLon(), random_double,cardp.get()));
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
////        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 7));

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
        addItems(antenas, bounds, googleMap);

        // Create a heat map tile provider, passing it the latlngs.
/*        if (latLngs.size() != 0) {
            HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                    .weightedData(latLngs)
                    //.radius(50)
                    .build();

            provider.setRadius(150);

            // Add a tile overlay to the map, using the heat map tile provider.
            googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
    }
*/
    }

    private void addItems(Map<String, Antena> antenas, LatLngBounds bounds, GoogleMap googleMap) {
        //Obtiene latitud y longitud del area visible del telefono
        LatLng northeast = bounds.northeast;
        Double boundLat = northeast.latitude;
        Double boundLong = northeast.longitude;

        LatLng southwest = bounds.southwest;
        Double boundLat2 = southwest.latitude;
        Double boundLong2 = southwest.longitude;

        int COLOR_RED_ARGB = 0x32F30821;
        int COLOR_YELLOW_ARGB = 0x32F3F308;
        int COLOR_GREEN_ARGB = 0x3200FF00;
        //double percent = 0;
        final int fillColor;

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
                                LatLng supIzq = new LatLng(0,0);
                                LatLng supDer = new LatLng(0,0);
                                LatLng infIzq = new LatLng(0,0);
                                LatLng infDer = new LatLng(0,0);

                                for (int i=0; i <= ints.getIntens(); i++) {
                                    switch (ints.getCardp()){
                                        case 'N':
                                            if (i == 0){
                                                infIzq = new LatLng(antena.getLat(),antena.getLon());
                                                infDer  = new LatLng(antena.getLat(),antena.getLon() + 0.00005);
                                                supDer = new LatLng(antena.getLat() + 0.0001, antena.getLon() + 0.00005);
                                                supIzq = new LatLng(antena.getLat() + 0.0001, antena.getLon());
                                            } else {
                                                infIzq = new LatLng(supIzq.latitude, supIzq.longitude - 0.00005);
                                                infDer = new LatLng(supDer.latitude, supDer.longitude + 0.00005);
                                                supDer = new LatLng(infDer.latitude + 0.0001,infDer.longitude);
                                                supIzq = new LatLng(infIzq.latitude + 0.0001,infIzq.longitude);
                                            }
                                            break;
                                        case 'S':
                                            if (i == 0){
                                                supIzq = new LatLng(antena.getLat(),antena.getLon());
                                                supDer = new LatLng(antena.getLat(),antena.getLon() + 0.00005);
                                                infDer = new LatLng(antena.getLat() - 0.0001, antena.getLon() + 0.00005);
                                                infIzq = new LatLng(antena.getLat() - 0.0001, antena.getLon());
                                            } else {
                                                supIzq = new LatLng(infIzq.latitude,infIzq.longitude - 0.00005);
                                                supDer = new LatLng(infDer.latitude,infDer.longitude + 0.00005);
                                                infDer = new LatLng(supDer.latitude - 0.0001, supDer.longitude);
                                                infIzq = new LatLng(supIzq.latitude - 0.0001, supIzq.longitude);
                                            }
                                            break;
                                        case 'O':
                                            if (i == 0){
                                                supDer = new LatLng(antena.getLat(),antena.getLon());
                                                supIzq = new LatLng(antena.getLat(),antena.getLon() - 0.0001);
                                                infIzq = new LatLng(antena.getLat() - 0.00005,antena.getLon() - 0.0001);
                                                infDer = new LatLng(antena.getLat() - 0.00005,antena.getLon());
                                            } else {
                                                supDer = new LatLng(supIzq.latitude + 0.00005,supIzq.longitude);
                                                supIzq = new LatLng(supDer.latitude,supDer.longitude - 0.0001);
                                                infDer = new LatLng(infIzq.latitude - 0.00005,infIzq.longitude);
                                                infIzq = new LatLng(infDer.latitude,infDer.longitude - 0.0001);
                                            }
                                            break;
                                        case 'E':
                                            if (i == 0){
                                                supIzq = new LatLng(antena.getLat(),antena.getLon());
                                                supDer = new LatLng(antena.getLat(),antena.getLon() + 0.0001);
                                                infDer = new LatLng(antena.getLat() - 0.00005,antena.getLon() + 0.0001);
                                                infIzq = new LatLng(antena.getLat() - 0.00005,antena.getLon());
                                            } else {
                                                supIzq = new LatLng(supDer.latitude + 0.00005,supDer.longitude);
                                                supDer = new LatLng(supIzq.latitude,supIzq.longitude + 0.0001);
                                                infIzq = new LatLng(infDer.latitude - 0.00005,infDer.longitude);
                                                infDer = new LatLng(infIzq.latitude,infIzq.longitude + 0.0001);
                                            }
                                            break;
                                    }

                                    Polygon polygon1 = googleMap.addPolygon(new PolygonOptions()
                                            .clickable(true)
                                            .add(
                                                    supIzq,
                                                    supDer,
                                                    infDer,
                                                    infIzq
                                            )
//                                    .fillColor(fillColor)
                                    .strokeWidth(0));
                                    double percent = (i * 100)/ints.getIntens();

                                    if (percent <= 33.33){
                                        polygon1.setFillColor(COLOR_RED_ARGB);
                                    } else {
                                        if (percent <= 66.66){
                                            polygon1.setFillColor(COLOR_YELLOW_ARGB);
                                        }else {
                                            polygon1.setFillColor(COLOR_GREEN_ARGB);
                                        }
                                    }
                                    // Store a data object with the polygon, used here to indicate an arbitrary type.
                                   // polygon1.setTag("alpha");
                                    // Style the polygon.
     //                               stylePolygon(polygon1);
                                }
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

/*    private static final int COLOR_WHITE_ARGB = 0xffffffff;
//    private static final int COLOR_GREEN_ARGB = 0x208000;
    private static final int COLOR_PURPLE_ARGB = 0xff81C784;
    private static final int COLOR_ORANGE_ARGB = 0xffF57F17;
    //private static final int COLOR_BLUE_ARGB = 0xffF9A825;
    private static final int COLOR_GREEN_ARGB = 0x3200FF00;
    private static final int COLOR_BLACK_ARGB = 0xff000000;

    private static final int PATTERN_GAP_LENGTH_PX = 20;
    private static final PatternItem DOT = new Dot();
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);

    private static final int POLYGON_STROKE_WIDTH_PX = 0;
    private static final int PATTERN_DASH_LENGTH_PX = 20;
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    // Create a stroke pattern of a gap followed by a dash.
    private static final List<PatternItem> PATTERN_POLYGON_ALPHA = Arrays.asList(GAP, DASH);

    // Create a stroke pattern of a dot followed by a gap, a dash, and another gap.
    private static final List<PatternItem> PATTERN_POLYGON_BETA =
            Arrays.asList(DOT, GAP, DASH, GAP);

    private void stylePolygon(Polygon polygon) {

        String type = "";
        // Get the data object stored with the polygon.
        if (polygon.getTag() != null) {
            type = polygon.getTag().toString();
        }

        List<PatternItem> pattern = null;
        int strokeColor = COLOR_BLACK_ARGB;
        int fillColor = COLOR_WHITE_ARGB;

        switch (type) {
            // If no type is given, allow the API to use the default.
            case "alpha":
                // Apply a stroke pattern to render a dashed line, and define colors.
             //   pattern = PATTERN_POLYGON_ALPHA;
             //   strokeColor = COLOR_GREEN_ARGB;
                fillColor = COLOR_GREEN_ARGB;
                break;
            case "beta":
                // Apply a stroke pattern to render a line of dots and dashes, and define colors.
             //   pattern = PATTERN_POLYGON_BETA;
             //   strokeColor = COLOR_ORANGE_ARGB;
             //   fillColor = COLOR_BLUE_ARGB;
                break;
        }

        //polygon.setStrokePattern(pattern);
        polygon.setStrokeWidth(POLYGON_STROKE_WIDTH_PX);
        //polygon.setStrokeColor(strokeColor);
        polygon.setFillColor(fillColor);
    }
 */
}