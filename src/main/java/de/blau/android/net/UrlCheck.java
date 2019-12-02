package de.blau.android.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.osm.Tags;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UrlCheck {

    private static final String DEBUG_TAG = UrlCheck.class.getName();

    private static final long TIMEOUT = 5000;

    public enum CheckStatus {
        HTTP, HTTPS, DOESNTEXIST, UNREACHABLE, MALFORMEDURL, INVALID
    };

    public static Result check(@NonNull String domain) {
        return connect(domain, true);
    }

    public static class Result {
        private final CheckStatus status;
        private final String url;
        
        public Result(@NonNull final CheckStatus status, @NonNull final String url) {
            this.status = status;
            this.url = url;
        }

        /**
         * @return the status
         */
        public CheckStatus getStatus() {
            return status;
        }

        /**
         * @return the url
         */
        public String getUrl() {
            return url;
        }
        
        public String toString() {
            return url + " " + status;
        }
    }
    
    private static Result connect(final @NonNull String domain, boolean https) {
        AsyncTask<Void, Void, Result> task = new AsyncTask<Void, Void, Result>() {
            @Override
            protected Result doInBackground(Void... params) {
                URL url = null;
                try {
                    String tempDomain = domain;
                    if (domain.toLowerCase().startsWith("http")) { // strip protocol
                        URL temp = new URL(domain);
                        tempDomain = temp.getHost() + "/" + temp.getPath();
                    }
                    url = new URL((https ? Tags.HTTPS_PREFIX : Tags.HTTP_PREFIX) + tempDomain);

                    Log.d(DEBUG_TAG, "checking url for " + url.toString());
                 
                    Request request = new Request.Builder().url(url).head().build();
                    OkHttpClient.Builder builder = App.getHttpClient().newBuilder().connectTimeout(100, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT,
                            TimeUnit.MILLISECONDS);
                    OkHttpClient client = builder.build();
                    Call readCall = client.newCall(request);
                    Response readCallResponse = readCall.execute();
                    if (readCallResponse.code() == HttpURLConnection.HTTP_OK) {
                        return new Result(https ? CheckStatus.HTTPS : CheckStatus.HTTP, url.toString());
                    }
                    return new Result(CheckStatus.INVALID, domain);
                } catch (MalformedURLException e) {
                    return new Result(CheckStatus.MALFORMEDURL, domain);
                } catch (UnknownHostException uhe) {
                    return new Result(CheckStatus.DOESNTEXIST, domain);
                } catch (IOException e) {
                    return new Result(CheckStatus.UNREACHABLE, url.toString());
                }
            }
        };
        task.execute();
        try {
            Result result = task.get(500, TimeUnit.MILLISECONDS);
            if (result.getStatus() != CheckStatus.HTTPS && https) {
                return connect(domain, false);
            }
            return result;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            return new Result(CheckStatus.UNREACHABLE, domain);
        }
    }
}
