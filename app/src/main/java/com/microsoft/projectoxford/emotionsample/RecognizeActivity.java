
package com.microsoft.projectoxford.emotionsample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.emotionsample.helper.ImageHelper;

import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class RecognizeActivity extends ActionBarActivity {

    private static final int REQUEST_SELECT_IMAGE = 0;

    public static String select ="";
    private Button mButtonSelectImage;

    private Uri mImageUri;

    private Bitmap mBitmap;

    private EditText mEditText;
    public TextView mtext;
     String disp = "Detected emotion: ";

    double emot[] = new double[200];
    int c =0,max =0;

    private EmotionServiceClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);

        if (client == null) {
            client = new EmotionServiceRestClient(getString(R.string.subscription_key));
        }

        mButtonSelectImage = (Button) findViewById(R.id.buttonSelectImage);
        mEditText = (EditText) findViewById(R.id.editTextResult);
        mtext = (TextView) findViewById(R.id.mTextView);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recognize, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void doRecognize() {
        mButtonSelectImage.setEnabled(false);

        try {
            new doRequest(false).execute();
        } catch (Exception e) {
            mEditText.append("Error encountered. Exception is: " + e.toString());
        }

        String faceSubscriptionKey = getString(R.string.faceSubscription_key);
        if (faceSubscriptionKey.equalsIgnoreCase("Please_add_the_face_subscription_key_here")) {
            mEditText.append("\n\nThere is no face subscription key in res/values/strings.xml");
        } else {

            try {
                new doRequest(true).execute();
            } catch (Exception e) {
                mEditText.append("Error encountered. Exception is: " + e.toString());
            }
        }
    }


    public void selectImage(View view) {
        mEditText.setText("");

        Intent intent;
        intent = new Intent(RecognizeActivity.this, com.microsoft.projectoxford.emotionsample.helper.SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    public void play(View view) {
        Intent intent;
        intent = new Intent(RecognizeActivity.this, com.microsoft.projectoxford.emotionsample.PlayActivity.class);
        startActivity(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("RecognizeActivity", "onActivityResult");
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {

                    mImageUri = data.getData();

                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri, getContentResolver());
                    if (mBitmap != null) {

                        ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                        imageView.setImageBitmap(mBitmap);
                        Log.d("RecognizeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());

                        doRecognize();
                    }
                }
                break;
            default:
                break;
        }
    }


    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");

        Gson gson = new Gson();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long startTime = System.currentTimeMillis();

        List<RecognizeResult> result = null;

        result = this.client.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);

        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));

        return result;
    }

    private List<RecognizeResult> processWithFaceRectangles() throws EmotionServiceException, com.microsoft.projectoxford.face.rest.ClientException, IOException {
        Log.d("emotion", "Do emotion detection with known face rectangles");
        Gson gson = new Gson();


        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long timeMark = System.currentTimeMillis();
        Log.d("emotion", "Start face detection using Face API");
        FaceRectangle[] faceRectangles = null;
        String faceSubscriptionKey = getString(R.string.faceSubscription_key);
        FaceServiceRestClient faceClient = new FaceServiceRestClient(faceSubscriptionKey);
        Face faces[] = faceClient.detect(inputStream, false, false, null);
        Log.d("emotion", String.format("Face detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));

        if (faces != null) {
            faceRectangles = new FaceRectangle[faces.length];

            for (int i = 0; i < faceRectangles.length; i++) {

                com.microsoft.projectoxford.face.contract.FaceRectangle rect = faces[i].faceRectangle;
                faceRectangles[i] = new com.microsoft.projectoxford.emotion.contract.FaceRectangle(rect.left, rect.top, rect.width, rect.height);
            }
        }

        List<RecognizeResult> result = null;
        if (faceRectangles != null) {
            inputStream.reset();

            timeMark = System.currentTimeMillis();
            Log.d("emotion", "Start emotion detection using Emotion API");

            result = this.client.recognizeImage(inputStream, faceRectangles);

            String json = gson.toJson(result);
            Log.d("result", json);

            Log.d("emotion", String.format("Emotion detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));
        }
        return result;
    }

    private class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {

        private Exception e = null;
        private boolean useFaceRectangles = false;

        public doRequest(boolean useFaceRectangles) {
            this.useFaceRectangles = useFaceRectangles;
        }

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            if (this.useFaceRectangles == false) {
                try {
                    return processWithAutoFaceDetection();
                } catch (Exception e) {
                    this.e = e;
                }
            } else {
                try {
                    return processWithFaceRectangles();
                } catch (Exception e) {
                    this.e = e;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            c=0;
            super.onPostExecute(result);

           if (this.useFaceRectangles == false) {
                mEditText.append("\n\nRecognizing emotions with auto-detected face rectangles...\n");
            } else {
                mEditText.append("\n\nRecognizing emotions with existing face rectangles from Face API...\n");
            }
            if (e != null) {
                mEditText.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
                if (result.size() == 0) {
                    mEditText.append("No emotion detected :(");
                } else {
                    Integer count = 1;
                    Bitmap bitmapCopy = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas faceCanvas = new Canvas(bitmapCopy);
                    faceCanvas.drawBitmap(mBitmap, 0, 0, null);
                    for (RecognizeResult r : result) {
                        mEditText.append(String.format("\nFace %1$d \n", count));
                        mEditText.append(String.format("\tAnger: %1$.5f\n", r.scores.anger));
                        emot[c++] = r.scores.anger;
                        mEditText.append(String.format("\tContempt: %1$.5f\n", r.scores.contempt));
                        emot[c++] = r.scores.contempt;
                        mEditText.append(String.format("\tDisgust: %1$.5f\n", r.scores.disgust));
                        emot[c++] = r.scores.disgust;
                        mEditText.append(String.format("\tFear: %1$.5f\n", r.scores.fear));
                        emot[c++] = r.scores.fear;
                        mEditText.append(String.format("\tHappiness: %1$.5f\n", r.scores.happiness));
                        emot[c++] = r.scores.happiness;
                        mEditText.append(String.format("\tNeutral: %1$.5f\n", r.scores.neutral));
                        emot[c++] = r.scores.neutral;
                        mEditText.append(String.format("\tSadness: %1$.5f\n", r.scores.sadness));
                        emot[c++] = r.scores.sadness;
                        mEditText.append(String.format("\tSurprise: %1$.5f\n", r.scores.surprise));
                        emot[c++] = r.scores.surprise;
                        count++;
                    }
                    ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                    imageView.setImageDrawable(new BitmapDrawable(getResources(), mBitmap));
                }

                for (int k = 0; k < c; k++) {
                    if (emot[k] > emot[max])
                        max = k;

                }

                if (max == 0) {
                    disp = "Detected emotion : Anger";
                    select="anger";
                }
                if (max == 1) {
                    disp = "Detected emotion : Contempt";
                    select="contempt";
                }
                if (max == 2) {
                    disp = "Detected emotion : Disgust";
                    select="disgust";
                }
                if (max == 3) {
                    disp = "Detected emotion : Fear";
                    select="fear";
                }
                if (max == 4) {
                    disp = "Detected emotion : Happiness";
                    select="happy";
                }
                if(max==5) {
                    disp = "Detected emotion : Neutral";
                    select="neutral";
                }
                if(max ==6) {
                    disp = "Detected emotion : Sadness";
                    select="sad";
                }
                if(max ==7) {
                    disp = "Detected emotion : Surprise";
                    select="surprise";

                }
                mtext.setText(disp);
            }

            mButtonSelectImage.setEnabled(true);
        }
    }
}
