/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.ybo.transportsrennes;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderStatus;
import fr.ybo.itineraires.CalculItineraires;
import fr.ybo.itineraires.modele.Adresse;
import fr.ybo.itineraires.schema.ItineraireReponse;
import fr.ybo.transportsrennes.activity.MenuAccueil;
import fr.ybo.transportsrennes.util.GsonUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class ItineraireRequete extends MenuAccueil.Activity implements LocationListener {

    /**
     * Le locationManager permet d'accéder au GPS du téléphone.
     */
    private LocationManager locationManager;

    private Location lastLocation;

    /**
     * Permet de mettre à jour les distances des stations par rapport à une
     * nouvelle position.
     *
     * @param location position courante.
     */
    @SuppressWarnings("unchecked")
    private void mettreAjoutLoc(Location location) {
        if (location != null && (lastLocation == null || location.getAccuracy() <= lastLocation.getAccuracy() + 50.0)) {
            lastLocation = location;
        }
    }

    public void onLocationChanged(Location arg0) {
        mettreAjoutLoc(arg0);
    }

    public void onProviderDisabled(String arg0) {
        desactiveGps();
        activeGps();
    }

    public void onProviderEnabled(String arg0) {
        desactiveGps();
        activeGps();
    }

    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
    }

    /**
     * Active le GPS.
     */
    private void activeGps() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        mettreAjoutLoc(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
        List<String> providers = locationManager.getProviders(criteria, true);
        boolean gpsTrouve = false;
        for (String providerName : providers) {
            locationManager.requestLocationUpdates(providerName, 10000L, 20L, this);
            if (providerName.equals(LocationManager.GPS_PROVIDER)) {
                gpsTrouve = true;
            }
        }
        if (!gpsTrouve) {
            Toast.makeText(getApplicationContext(), getString(R.string.activeGps), Toast.LENGTH_SHORT).show();
        }
    }

    private void desactiveGps() {
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeGps();
    }

    @Override
    protected void onPause() {
        desactiveGps();
        super.onPause();
    }

    private Calendar calendar;

    private TextView dateItineraire;
    private TextView heureItineraire;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.itinerairerequete);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        calendar = Calendar.getInstance();
        dateItineraire = (TextView) findViewById(R.id.dateItineraire);
        heureItineraire = (TextView) findViewById(R.id.heureItineraire);
        majTextViews();
        dateItineraire.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showDialog(DATE_DIALOG_ID);
            }
        });
        heureItineraire.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showDialog(TIME_DIALOG_ID);
            }
        });
        Button boutonTerminer = (Button) findViewById(R.id.itineraireTermine);
        boutonTerminer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                terminer();
            }
        });
    }

    private void terminer() {
        String adresseDepart = null;
        Editable textDepart = ((EditText) findViewById(R.id.adresseDepart)).getText();
        if (textDepart.length() > 0) {
            adresseDepart = textDepart.toString();
        }
        Editable textArrivee = ((EditText) findViewById(R.id.adresseArrivee)).getText();
        String adresseArrivee = null;
        if (textArrivee.length() > 0) {
            adresseArrivee = textArrivee.toString();
        }
        if ((adresseDepart == null || adresseArrivee == null) && (lastLocation == null || lastLocation.getAccuracy() > 50)) {
            Toast.makeText(this, R.string.erreur_gpsPasPret, Toast.LENGTH_LONG).show();
        } else {
            geoCoderAdresse(adresseDepart, adresseArrivee);
        }
    }

    private void geoCoderAdresse(final String adresseDepart, final String adresseArrivee) {
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog progressDialog;
            private boolean erreur;
            private GeocodeResponse reponseDepart;
            private GeocodeResponse reponseArrivee;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog = ProgressDialog.show(ItineraireRequete.this, "", getString(R.string.geocodageAdresseDepart), true);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                Geocoder geocoder = new Geocoder();
                if (adresseDepart != null) {
                    GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(adresseDepart).setLanguage("fr").getGeocoderRequest();
                    reponseDepart = geocoder.geocode(geocoderRequest);
                    if (reponseDepart == null || reponseDepart.getStatus() != GeocoderStatus.OK) {
                        erreur = true;
                        return null;
                    }
                }
                if (adresseArrivee != null) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressDialog.setMessage(getString(R.string.geocodageAdresseArrivee));
                        }
                    });
                    GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(adresseArrivee).setLanguage("fr").getGeocoderRequest();
                    reponseArrivee = geocoder.geocode(geocoderRequest);
                    if (reponseArrivee == null || reponseArrivee.getStatus() != GeocoderStatus.OK) {
                        erreur = true;
                        return null;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                progressDialog.dismiss();
                if (erreur) {
                    Toast.makeText(ItineraireRequete.this, R.string.erreur_geocodage, Toast.LENGTH_LONG).show();
                } else {
                    traiterReponseGeoCodage(reponseDepart, reponseArrivee);
                }
            }
        }.execute();
    }

    private void traiterReponseGeoCodage(GeocodeResponse reponseDepart, GeocodeResponse reponseArrivee) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean erreur = false;
        if (reponseDepart != null && reponseDepart.getResults().isEmpty()) {
            stringBuilder.append(getString(R.string.erreur_pasAdresseDepart));
            stringBuilder.append('\n');
            erreur = true;
        }
        if (reponseArrivee != null && reponseArrivee.getResults().isEmpty()) {
            stringBuilder.append(getString(R.string.erreur_pasAdresseArrivee));
            erreur = true;
        }
        if (erreur) {
            Toast.makeText(this, stringBuilder.toString(), Toast.LENGTH_LONG).show();
        } else {
            if (reponseDepart != null && reponseDepart.getResults().size() > 1 || reponseArrivee != null && reponseArrivee.getResults().size() > 1) {
                traiterAdresseMultiple(reponseDepart, reponseArrivee);
            } else {
                calculItineraire(reponseDepart, reponseArrivee);
            }
        }
    }

    private void traiterAdresseMultiple(GeocodeResponse reponseDepart, GeocodeResponse reponseArrivee) {
        Toast.makeText(this,R.string.erreur_multipleAdresses, Toast.LENGTH_LONG).show();
    }

    private void calculItineraire(final GeocodeResponse reponseDepart, final GeocodeResponse reponseArrivee) {
        final Adresse adresseDepart = new Adresse();
        final Adresse adresseArrivee = new Adresse();
        if (reponseDepart != null) {
            adresseDepart.latitude = reponseDepart.getResults().get(0).getGeometry().getLocation().getLat().doubleValue();
            adresseDepart.longitude = reponseDepart.getResults().get(0).getGeometry().getLocation().getLng().doubleValue();
        } else {
            adresseDepart.latitude = lastLocation.getLatitude();
            adresseDepart.longitude = lastLocation.getLongitude();
        }
        if (reponseArrivee != null) {
            adresseArrivee.latitude = reponseArrivee.getResults().get(0).getGeometry().getLocation().getLat().doubleValue();
            adresseArrivee.longitude = reponseArrivee.getResults().get(0).getGeometry().getLocation().getLng().doubleValue();
        } else {
            adresseArrivee.latitude = lastLocation.getLatitude();
            adresseArrivee.longitude = lastLocation.getLongitude();
        }
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog progressDialog;
            private ItineraireReponse reponse;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog = ProgressDialog.show(ItineraireRequete.this, "", getString(R.string.calculItineraire), true);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                reponse = CalculItineraires.getInstance().calculItineraires(adresseDepart, adresseArrivee, calendar);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                progressDialog.dismiss();
                if (reponse.getErreur() != null) {
                    Toast.makeText(ItineraireRequete.this, R.string.erreur_calculItineraires, Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(ItineraireRequete.this, Itineraires.class);
                    intent.putExtra("itinerairesReponse", GsonUtil.getInstance().toJson(reponse));
                    int heureDepart = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
                    intent.putExtra("heureDepart", heureDepart);
                    startActivity(intent);
                }
            }
        }.execute();
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    private void majTextViews() {
        dateItineraire.setText(DATE_FORMAT.format(calendar.getTime()));
        heureItineraire.setText(TIME_FORMAT.format(calendar.getTime()));
    }

    private static final int DATE_DIALOG_ID = 0;
    private static final int TIME_DIALOG_ID = 1;

    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            majTextViews();
        }
    };

    private TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {

        public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            majTextViews();
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DATE_DIALOG_ID) {
            return new DatePickerDialog(this, mDateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
        } else if (id == TIME_DIALOG_ID) {
            return new TimePickerDialog(this, timeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        }
        return null;
    }
}