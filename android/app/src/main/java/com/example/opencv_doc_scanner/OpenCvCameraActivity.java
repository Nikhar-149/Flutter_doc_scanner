package com.example.opencv_doc_scanner;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.widget.Button;
import android.widget.Toast;

import android.content.Intent; // For Intent to navigate between activities
import android.os.Bundle; // For activity lifecycle methods like onCreate
import android.view.View; // For handling views and click events
import android.widget.ImageView; // For the ImageView component
import androidx.appcompat.app.AppCompatActivity; // If extending AppCompatActivity
import com.example.opencv_doc_scanner.DocumentScanner;

public class OpenCvCameraActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    // Declare global variables for stability tracking
    private int consecutiveFramesWithDocument = 0;
    private final int FRAMES_THRESHOLD = 30; // Number of frames to confirm stability
    private boolean isCapturing = false; // Prevent multiple captures during one stable period

    private List<String> capturedImages = new ArrayList<>(); // Declare the captured images list
    private boolean isToastVisible = false; // To prevent overlapping toasts

    // Initialize DocumentScanner and get the quadrilateral
    DocumentScanner scanner = new DocumentScanner();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "called onCreate");

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "OpenCV initialization failed!");
            return;
        }

        Log.i(TAG, "OpenCV loaded successfully");

        // Set up the layout
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_opencv_camera);

        // Camera view setup
        
        mOpenCvCameraView = findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // // Capture button setup
        // Button captureButton = findViewById(R.id.capture_button);
        // captureButton.setOnClickListener(v -> {
        //     isCapturing = true;
        //     Toast.makeText(OpenCvCameraActivity.this, "Capture button clicked!", Toast.LENGTH_SHORT).show();
        // });

        // ImageView previewArea = findViewById(R.id.preview_area);
        // previewArea.setOnClickListener(v -> {
        // // Intent intent = new Intent(OpenCvCameraActivity.this,
        // CapturedImagesActivity.class);
        // intent.putStringArrayListExtra("captured_images", new
        // ArrayList<>(capturedImages));
        // startActivity(intent);
        // });

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.enableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        // You can initialize any matrices or variables here
    }

    @Override
    public void onCameraViewStopped() {
        // Release allocated resources
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        // Create a copy of the original image
        Mat rgbaCopy = rgba.clone();

        try {

            // Initialize DocumentScanner and get the quadrilateral
            List<MatOfPoint> contours = scanner.findContours(rgba);

            // // Detect the largest rectangle
            MatOfPoint largestRectangle = scanner.findLargestRectangle(contours, rgba.size());

            if (largestRectangle != null) {

                scanner.drawDocument(rgba, largestRectangle);

                // Calculate the bounding rectangle
                Rect boundingRect = Imgproc.boundingRect(largestRectangle);

                // Check if document is stable
                // Check if the document is too far (small area) or well-aligned
                if (scanner.isTooFar(boundingRect, rgba)) {
                    showToast("Move closer to the document!");

                } else if (scanner.isDocumentStable(largestRectangle, consecutiveFramesWithDocument,
                        FRAMES_THRESHOLD)) {
                    // Trigger capture if not already capturing
                    if (!isCapturing) {
                        isCapturing = true; // Lock capturing
                        showToast("Document stable! Capturing...");
                        captureDocument(rgbaCopy, largestRectangle); // Save the frame
                    }
                } else {
                    // runOnUiThread(() ->
                    // Toast.makeText(this, "Hold still! Align the document properly.",
                    // Toast.LENGTH_SHORT).show()
                    // );
                    isCapturing = false; // Reset capturing when unstable
                }
            } else {
                // Reset stability counter if no rectangle is found
                consecutiveFramesWithDocument = 0;
                isCapturing = false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCameraFrame: " + e.getMessage());
        }

        return rgba;
    }

    private void captureDocument(Mat rgba, MatOfPoint rectangle) {
        Mat cropped = cropImage(rgba, rectangle);

        // Save the captured document
        saveFrame(enhanceImage(cropped));

        // Reset stability counter
        consecutiveFramesWithDocument = 0;

        // Provide visual feedback
        runOnUiThread(() -> Toast.makeText(this, "Document captured!", Toast.LENGTH_SHORT).show());

    }

    /**
     * Prevents overlapping toasts by checking if a toast is already visible.
     */
    private void showToast(String message) {
        if (!isToastVisible) {
            isToastVisible = true;
            runOnUiThread(() -> {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(() -> isToastVisible = false, 1500); // Toast visibility duration
            });
        }
    }

    // Crop the image using the largest rectangle
    private Mat cropImage(Mat image, MatOfPoint rectangle) {
        // Convert the rectangle points to a MatOfPoint2f for the perspective transform
        MatOfPoint2f points2f = new MatOfPoint2f(rectangle.toArray());

        // Define destination points (a square or rectangle)
        Point[] destPoints = new Point[] {
                new Point(0, 0),
                new Point(image.cols() - 1, 0),
                new Point(image.cols() - 1, image.rows() - 1),
                new Point(0, image.rows() - 1)
        };
        MatOfPoint2f dst = new MatOfPoint2f(destPoints);

        // Get the perspective transformation matrix
        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(points2f, dst);

        // Apply the perspective warp (crop)
        Mat cropped = new Mat();
        Imgproc.warpPerspective(image, cropped, perspectiveTransform, new Size(image.cols(), image.rows()));

        // Optionally, you can crop further to the bounding box of the document
        Rect roi = new Rect(0, 0, image.cols(), image.rows());
        return new Mat(cropped, roi);
    }

    public static Mat enhanceImage(Mat image) {
        // Read the image

        // Apply image enhancements
        // 1. Increase brightness and contrast
        Mat brightenedImage = new Mat();
        image.convertTo(brightenedImage, -1, 1.2, 30); // alpha=1.2 (contrast), beta=30 (brightness)

        // 2. Apply sharpening
        Mat kernel = new Mat(3, 3, CvType.CV_32F) {
            {
                put(0, 0, 0);
                put(0, 1, -1);
                put(0, 2, 0);

                put(1, 0, -1);
                put(1, 1, 5);
                put(1, 2, -1);

                put(2, 0, 0);
                put(2, 1, -1);
                put(2, 2, 0);
            }
        };

        Mat sharpenedImage = new Mat();
        Imgproc.filter2D(brightenedImage, sharpenedImage, image.depth(), kernel);

        // 3. Apply Gaussian blur (optional, for noise reduction)
        Mat blurredImage = new Mat();
        Imgproc.GaussianBlur(sharpenedImage, blurredImage, new Size(3, 3), 0);

        return blurredImage;
    }

    /**
     * Optimized method to save the frame.
     */
    private void saveFrame(Mat frame) {
        String filename = getExternalFilesDir(null) + "/captured_frame_" + System.currentTimeMillis() + ".png";
        Mat bgrFrame = new Mat();
        Imgproc.cvtColor(frame, bgrFrame, Imgproc.COLOR_RGBA2BGR);
        boolean success = Imgcodecs.imwrite(filename, bgrFrame);

        if (success) {
            Log.i(TAG, "Frame saved at: " + filename);
            runOnUiThread(() -> {
                Toast.makeText(this, "Captured: " + filename, Toast.LENGTH_SHORT).show();
                capturedImages.add(filename); // Add to batch
            });
        } else {
            Log.e(TAG, "Failed to save frame.");
        }

    }

}
