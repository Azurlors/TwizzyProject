
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.features2d.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import static org.opencv.imgproc.Imgproc.cvtColor;

public class PatternMatching {

    public static Mat lectureImage(String fileName) {
        File f = new File(fileName);
        return Imgcodecs.imread(f.getAbsolutePath());
    }

    public static void imShow(String title, Mat img) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", img, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        BufferedImage bufferedImage = null;
        try {
            InputStream in = new ByteArrayInputStream((byteArray));
            bufferedImage = ImageIO.read(in);
            JFrame frame = new JFrame();
            frame.setTitle(title);
            frame.getContentPane().add(new JLabel(new ImageIcon(bufferedImage)));
            frame.pack();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static BufferedImage imGetFromMat(Mat img) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", img, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        BufferedImage bufferedImage = null;
        try {
            InputStream in = new ByteArrayInputStream((byteArray));
            bufferedImage = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bufferedImage;
    }
    public static Mat thresholdRed(Mat src) {
        Mat output = Mat.zeros(src.size(), src.type());
        cvtColor(src, output, Imgproc.COLOR_BGR2HSV);
        Mat threshold_img_1 = new Mat();
        Mat threshold_img_2 = new Mat();

        Core.inRange(output, new Scalar(0, 40, 40), new Scalar(10, 255, 255), threshold_img_1);
        Core.inRange(output, new Scalar(170, 40, 40), new Scalar(180, 255, 255), threshold_img_2);

        Mat threshold_img = new Mat();
        Core.bitwise_or(threshold_img_1, threshold_img_2, threshold_img);
        //imShow("Red Circle", threshold_img);

        Imgproc.GaussianBlur(threshold_img, threshold_img, new Size(9, 9), 2, 2);
        //imShow("Smooth Red Circle", threshold_img);
        return threshold_img;

    }

    public static List<MatOfPoint> outlines(Mat threshold_img) {
        int threshold = 100;
        Mat canny_output = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        MatOfInt4 hierarchy = new MatOfInt4();
        Imgproc.Canny(threshold_img, canny_output, threshold, threshold * 2);
        Imgproc.findContours(canny_output, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat drawing = Mat.zeros(canny_output.size(), CvType.CV_8UC3);
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(255, 255, 255);
            Imgproc.drawContours(drawing, contours, i, color, 1, 8, hierarchy, 0, new Point());
        }
        //imShow("Contours", drawing);
        return contours;
    }

    public static void circlesDetection(List<MatOfPoint> outlines_img, Mat src) {
        Mat circle = Mat.zeros(src.size(), src.type());
        MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        float[] radius = new float[1];
        Point center = new Point();
        for (MatOfPoint outline : outlines_img) {
            double contourArea = Imgproc.contourArea(outline);
            matOfPoint2f.fromList(outline.toList());
            Imgproc.minEnclosingCircle(matOfPoint2f, center, radius);

            if ((contourArea / (Math.PI * radius[0] * radius[0])) >= 0.7) {
                Imgproc.circle(circle, center, (int) radius[0], new Scalar(0, 255, 0), 2);
            }
        }
        //imShow("Circle detection ", circle);
    }

    public static List<Object> circlesDetectionExtraction(List<MatOfPoint> outlines_img, Mat src) {
        List<Object> ballsAndPoints= new ArrayList<>();
        List<Point> points =new ArrayList<>();
        List<Mat> balls = new ArrayList<>();
        MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        float[] radius = new float[1];
        Point center = new Point();
        for (MatOfPoint outline : outlines_img) {
            double contourArea = Imgproc.contourArea(outline);
            matOfPoint2f.fromList(outline.toList());
            Imgproc.minEnclosingCircle(matOfPoint2f, center, radius);
            if ((contourArea / (Math.PI * radius[0] * radius[0])) >= 0.9) {
                Imgproc.circle(src, center, (int) radius[0], new Scalar(0, 255, 0), 2);
                Rect rect = Imgproc.boundingRect(outline);
                Point point =new Point(rect.x, rect.y);
                points.add(point);
                Imgproc.rectangle(src, point,
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(0, 255, 0), 2);
                Mat tmp = src.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width);
                Mat ball = Mat.zeros(tmp.size(), tmp.type());
                tmp.copyTo(ball);

                //imShow("Ball", src);
                balls.add(ball);
            }
        }
        ballsAndPoints.add(balls);
        ballsAndPoints.add(points);

        return ballsAndPoints;
    }

    public static Mat scalingDetectedRoadSign(Mat ball, Mat sRoadSign) {
        Mat sObject = new Mat();
        Imgproc.resize(ball, sObject, sRoadSign.size());
        Mat grayObject = new Mat(sObject.rows(), sObject.cols(), sObject.type());
        cvtColor(sObject, grayObject, Imgproc.COLOR_BGRA2GRAY);
        Core.normalize(grayObject, grayObject, 0, 255, Core.NORM_MINMAX);
        return grayObject;
    }

    public static Mat scalingPatternRoadSign(Mat sRoadSign) {
        Mat sRoadSignScaled = new Mat(sRoadSign.rows(), sRoadSign.cols(), sRoadSign.type());
        cvtColor(sRoadSign, sRoadSignScaled, Imgproc.COLOR_BGRA2GRAY);
        Core.normalize(sRoadSignScaled, sRoadSignScaled, 0, 255, Core.NORM_MINMAX);
        return sRoadSignScaled;
    }


    public static double matching2(Mat detectedSign,Mat sRoadSign) {
        Mat detectedSignScaled = scalingDetectedRoadSign(detectedSign, sRoadSign);
        Mat sRoadSignScaled = scalingPatternRoadSign(sRoadSign);
        Mat result1 = new Mat();
        Imgproc.matchTemplate(detectedSignScaled, sRoadSignScaled, result1, Imgproc.TM_CCOEFF_NORMED);

        double score = Core.minMaxLoc(result1).maxVal;
        System.out.println("Score de similarité pour l'image : " + score);
        return score;
    }
    public static BufferedImage bestMatches(File selectedFile, List<String> patternSigns){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Mat testedImage = Imgcodecs.imread(selectedFile.getAbsolutePath());

        Mat threshold_img = thresholdRed(testedImage);
        java.util.List<MatOfPoint> outlines_img = outlines(threshold_img);
        circlesDetection(outlines_img, testedImage);
        List<Object> pointAndSign = circlesDetectionExtraction(outlines_img, testedImage);
        List<Mat> detectedSigns = (List<Mat>)pointAndSign.get(0);
        List<Point> points = (List<Point>)pointAndSign.get(1);
        int c=0;
        for (Mat detectedSign : detectedSigns) {
            List<Double> scores = new ArrayList<>();
            List<String> matchedPatterns = new ArrayList<>(); // keep track of matched patterns
            for (String patternSign : patternSigns) {
                Mat testedPatternSign = lectureImage(patternSign);
                //float matches1 = matching(detectedSign, testedPatternSign);
                //System.out.println(matches1);
                double score = matching2(detectedSign, testedPatternSign);
                scores.add(score);
                matchedPatterns.add(patternSign);
            }
            // Select the best match
            int bestSignIndex = maxList(scores);
            String bestSignText = matchedPatterns.get(bestSignIndex);

            // Draw a rectangle around the detected sign
            // Draw the text showing the best match

            int[] baseline = new int[1];
            Scalar color = new Scalar(0, 255, 0); // green

            Size textSize = Imgproc.getTextSize(bestSignText, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, 2, baseline);
            Rect textRect = new Rect((int)points.get(c).x, (int)points.get(c).y - (int)textSize.height, (int)textSize.width, (int)textSize.height + baseline[0]);
            Imgproc.rectangle(testedImage, textRect.tl(), textRect.br(), color, -1);
            Imgproc.putText(testedImage, bestSignText, points.get(c), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 255), 2);

            System.out.println(bestSignText + " is the most likely to be correct.");
            c=c+1;

        }
        //imShow("result",testedImage);
        return imGetFromMat(testedImage);
    }
    private static int maxList(List<Double> heights) {
        Double max = heights.get(0) ;
        int maxIndex = 0;
        for (int j = 1; j < heights.size(); j++) {
            if(max<heights.get(j)){
                max=heights.get(j);
                maxIndex=j;
            }
        }
        return maxIndex;
    }
    private static int minList(List<Double> norm) {
        Double min = norm.get(0) ;
        int minIndex = 0;
        for (int j = 1; j < norm.size(); j++) {
            if(min>norm.get(j)){
                min=norm.get(j);
                minIndex=j;
            }
        }
        return minIndex;
    }

    public static float matching(Mat detectedSign,Mat sRoadSign) {
        Mat ballsScaled = scalingDetectedRoadSign(detectedSign, sRoadSign);
        Mat sRoadSignScaled = scalingPatternRoadSign(sRoadSign);
        ORB orbDetector = ORB.create();
        ORB orbExtractor = ORB.create();

        MatOfKeyPoint sRoadSignKeyPoints = new MatOfKeyPoint();
        orbDetector.detect(sRoadSignScaled,sRoadSignKeyPoints);
        Mat sRoadSignDescriptor = new Mat(sRoadSign.rows(), sRoadSign.cols(), sRoadSign.type());
        orbExtractor.compute(sRoadSignScaled,sRoadSignKeyPoints,sRoadSignDescriptor);


        Mat sObject = new Mat();
        Imgproc.resize(detectedSign, sObject, sRoadSign.size());

        MatOfKeyPoint objectKeyPoints = new MatOfKeyPoint();
        orbDetector.detect(ballsScaled,objectKeyPoints);
        Mat objectDescriptor = new Mat(detectedSign.rows(), detectedSign.cols(), detectedSign.type());
        orbExtractor.compute(ballsScaled,objectKeyPoints,objectDescriptor);

        MatOfDMatch match = new MatOfDMatch();
        DescriptorMatcher matcher =DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
        matcher.match(sRoadSignDescriptor,objectDescriptor,match);

        Mat matchedImage = new Mat(sRoadSign.rows(),sRoadSign.cols()*2,sRoadSign.type());
        Features2d.drawMatches(sRoadSign,sRoadSignKeyPoints,sObject,objectKeyPoints,match,matchedImage);
        imShow("matched Sign", matchedImage);

        //System.out.println(match.height());
        System.out.println(match.dump());

        float avgDistance = 0;
        float squareAvgDistance = 0;
        float ecartType=0;
        List<DMatch> matchesList = match.toList();

        for (DMatch dMatch : matchesList) {
            avgDistance = avgDistance + dMatch.distance;
            squareAvgDistance = squareAvgDistance + dMatch.distance*dMatch.distance;
        }
        avgDistance = avgDistance /matchesList.size();
        squareAvgDistance=squareAvgDistance/matchesList.size();

        ecartType=(float)Math.sqrt(squareAvgDistance-avgDistance*avgDistance);
        return ecartType;
    }
}
