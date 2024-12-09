package  com.example.opencv_doc_scanner;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DocumentScanner {

//    private static final float HOT_AREA_MARGIN = 0.1f;  // 10% margin
private static final float HOT_AREA_MARGIN = 0.1f; // Reduce margin to 5%

    // Margin for hot area boundaries


    // Class for holding quadrilateral information
    public static class Quadrilateral {
        public MatOfPoint contour;
        public Point[] points;

        public Quadrilateral(MatOfPoint contour, Point[] points) {
            this.contour = contour;
            this.points = points;
        }
    }








    /**
     * Checks if the detected document is stable for a given number of frames.
     */
    public boolean isDocumentStable(MatOfPoint rectangle, int consecutiveFramesWithDocument, int FRAMES_THRESHOLD) {
        // Get the bounding rectangle
        Rect boundingRect = Imgproc.boundingRect(rectangle);
        double aspectRatio = (double) boundingRect.width / boundingRect.height;

        // Ensure aspect ratio is within a reasonable range for a document
        if (aspectRatio > 0.7 && aspectRatio < 0.8) {
            consecutiveFramesWithDocument++;
        } else {
            consecutiveFramesWithDocument = 0;
        }

        // Return true if stable for enough frames
        return consecutiveFramesWithDocument >= FRAMES_THRESHOLD;
    }








     List<MatOfPoint> findContours(Mat rgba){


         // Preprocess the frame: Grayscale and Edge Detection
         Mat gray = new Mat();
         Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY);
         Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);
         Mat edges = new Mat();
         Imgproc.Canny(gray, edges, 75, 200);

         // Find contours
         List<MatOfPoint> contours = new ArrayList<>();
         Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
         Collections.sort(contours, (lhs, rhs) -> Double.valueOf(Imgproc.contourArea(rhs)).compareTo(Imgproc.contourArea(lhs)));

         return contours;

    }

    void drawDocument (Mat rgba,  MatOfPoint largestRectangle){

// Draw the rectangle on the screen for feedback
        Imgproc.drawContours(rgba, Collections.singletonList(largestRectangle), -1, new Scalar(0, 255, 0), 5);


    }





    // Find the largest rectangle-like contour
    MatOfPoint findLargestRectangle(List<MatOfPoint> contours, Size imageSize) {
        MatOfPoint largest = null;
        double maxArea = 0;

        // Iterate over the contours
        for (MatOfPoint contour : contours) {
            // Skip contours with less than 4 points
            if (contour.size().height < 4) continue;

            double area = Imgproc.contourArea(contour);

            // Consider contours with significant area
            if (area > 5000) {
                // Convert contour to MatOfPoint2f for processing
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                double peri = Imgproc.arcLength(contour2f, true);
                MatOfPoint2f approx2f = new MatOfPoint2f();

                // Approximate the contour to a polygon
                Imgproc.approxPolyDP(contour2f, approx2f, 0.02 * peri, true);
                MatOfPoint approx = new MatOfPoint(approx2f.toArray());

                // Check if the contour has 4 points (a quadrilateral)
                if (approx.total() == 4) {
                    // Check if this rectangle is the largest and inside the hot area
                    if (area > maxArea && insideHotArea(approx.toArray(), imageSize)) {
                        largest = approx;
                        maxArea = area;
                    }
                }
            }
        }

        // Return the largest rectangle or null if not found
        return largest;
    }







    // Sort the points in the order: top-left, top-right, bottom-right, bottom-left
    private Point[] sortPoints( Point[] src ) {

        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Point[] result = { null , null , null , null };

        Comparator<Point> sumComparator = (lhs, rhs) -> Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);

        Comparator<Point> diffComparator = (lhs, rhs) -> Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal diference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal diference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }



    // Check if the quadrilateral is inside the valid "hot area"
    private boolean insideHotArea(Point[] rp, Size size) {
        // Define the hot area as a percentage of the image size (you can tweak the margin)
        int[] hotArea = getHotArea((int) size.width, (int) size.height,HOT_AREA_MARGIN);
        System.out.println("Hot Area: " + Arrays.toString(hotArea));

//        return true; // Temporarily allow all quadrilaterals
        return (rp[0].x >= hotArea[0] && rp[0].y >= hotArea[1]  // Top-left corner
                && rp[1].x <= hotArea[2] && rp[1].y >= hotArea[1]  // Top-right corner
                && rp[2].x <= hotArea[2] && rp[2].y <= hotArea[3]  // Bottom-right corner
                && rp[3].x >= hotArea[0] && rp[3].y <= hotArea[3]); // Bottom-left corner

//        int[] hotArea = getHotArea((int) size.width, (int) size.height, HOT_AREA_MARGIN);
//        int tolerance = 10; // Allow 10-pixel tolerance
//
//        return (rp[0].x >= hotArea[0] - tolerance && rp[0].y >= hotArea[1] - tolerance
//                && rp[1].x <= hotArea[2] + tolerance && rp[1].y >= hotArea[1] - tolerance
//                && rp[2].x <= hotArea[2] + tolerance && rp[2].y <= hotArea[3] + tolerance
//                && rp[3].x >= hotArea[0] - tolerance && rp[3].y <= hotArea[3] + tolerance);


    }

    // Define the hot area based on image size and margins (for camera preview)
    // Allow margin to be set dynamically
    private int[] getHotArea(int width, int height, float marginFactor) {
        int marginX = (int) (width * marginFactor); // Horizontal margin
        int marginY = (int) (height * marginFactor); // Vertical margin

        return new int[]{
                marginX,               // Left margin (x-min)
                marginY,               // Top margin (y-min)
                width - marginX,       // Right margin (x-max)
                height - marginY       // Bottom margin (y-max)
        };
    }


/**
     * Determines if the detected document is too far by checking its bounding area.
     */
    public boolean isTooFar(Rect boundingRect,Mat rgba) {
        double documentArea = boundingRect.width * boundingRect.height;

        // Adjust the threshold based on camera resolution
        double minAreaThreshold = 0.2 * rgba.size().area(); // Example: 20% of the frame area
        return documentArea < minAreaThreshold;
    }


}