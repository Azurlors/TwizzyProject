
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.features2d.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Mat mat = lectureImage("s_p10.png");
        ImShow("test",mat);
        //convPlusDot(mat);
        //displayChannels(mat);
        //displayColorsChannels(mat);
        //changeSpaceColor(mat);
        Mat threshold_img = threshodRed(mat);
        List<MatOfPoint> outlines_img = outlines(threshold_img);
        circlesDetection(outlines_img, mat);
        List<Mat> balls = circlesDetectionExtraction(outlines_img, mat);

        Mat sroadSign = lectureImage("ref30.jpg");
        matching(balls,sroadSign);
        Mat sroadSign2 = lectureImage("refdouble.jpg");
        matching(balls,sroadSign2);

    }

    public static Mat lectureImage(String fichier) {
        File f = new File(fichier);
        Mat m = Imgcodecs.imread(f.getAbsolutePath());
        return m;
    }

    public static Mat rgbToGrey(Mat src) {
        Mat dst = new Mat();
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGB2GRAY);
        return dst;
    }

    public static void convPlusDot(Mat src) {
        for (int i = 0; i < src.height(); i++) {
            for (int j = 0; j < src.width(); j++) {
                double[] BGR = src.get(i, j);
                if (BGR[0] == 255 && BGR[1] == 255 && BGR[2] == 255) {
                    System.out.print(".");
                } else {
                    System.out.print("+");
                }
            }
            System.out.println("");
        }
    }

    public static void ImShow(String title, Mat img) {
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

    public static void displayChannels(Mat src) {
        Vector<Mat> channels = new Vector<Mat>();
        Core.split(src, channels);
        for (int i = 0; i < channels.size(); i++) {
            ImShow(Integer.toString(i), channels.get(i));
        }
    }

    public static void displayColorsChannels(Mat src) {
        Vector<Mat> channels = new Vector<Mat>();
        Core.split(src, channels);
        Mat dst = Mat.zeros(src.size(), src.type());
        Vector<Mat> chans = new Vector<Mat>();
        Mat empty = Mat.zeros(src.size(), CvType.CV_8UC1);
        for (int i = 0; i < channels.size(); i++) {
            chans.removeAllElements();
            for (int j = 0; j < channels.size(); j++) {
                if (j != i) {
                    chans.add(empty);
                } else {
                    chans.add(channels.get(i));
                }
            }
            Core.merge(chans, dst);
            ImShow(Integer.toString(i), dst);
        }
    }

    public static void changeSpaceColor(Mat src) {
        Mat output = Mat.zeros(src.size(), src.type());
        Imgproc.cvtColor(src, output, Imgproc.COLOR_BGR2HSV);
        ImShow("HSV", output);
        Vector<Mat> channels = new Vector<Mat>();
        Core.split(src, channels);
        double hsv_values[][] = {{1, 255, 255}, {179, 1, 255}, {179, 0, 1}};
        for (int i = 0; i < 3; i++) {
            ImShow(Integer.toString(i) + "-HSV", channels.get(i));
            Mat chans[] = new Mat[3];
            for (int j = 0; j < 3; j++) {
                Mat empty = Mat.ones(src.size(), CvType.CV_8UC1);
                Mat comp = Mat.ones(src.size(), CvType.CV_8UC1);
                Scalar v = new Scalar(hsv_values[i][j]);
                Core.multiply(empty, v, comp);
                chans[j] = comp;
            }
            chans[i] = channels.get(i);
            Mat dst = Mat.zeros(output.size(), output.type());
            Mat res = Mat.ones(dst.size(), dst.type());
            Core.merge(Arrays.asList(chans), dst);
            Imgproc.cvtColor(dst, res, Imgproc.COLOR_HSV2BGR);
            ImShow(Integer.toString(i), res);
        }
    }

    public static Mat threshodRed(Mat src) {
        Mat output = Mat.zeros(src.size(), src.type());
        Imgproc.cvtColor(src, output, Imgproc.COLOR_BGR2HSV);
        Mat threshold_img_1 = new Mat();
        Mat threshold_img_2 = new Mat();

        Core.inRange(output, new Scalar(0, 40, 40), new Scalar(15, 255, 255), threshold_img_1);
        Core.inRange(output, new Scalar(170, 40, 40), new Scalar(180, 255, 255), threshold_img_2);

        Mat threshold_img = new Mat();
        Core.bitwise_or(threshold_img_1, threshold_img_2, threshold_img);
        ImShow("Cercle Rouge", threshold_img);

        Imgproc.GaussianBlur(threshold_img, threshold_img, new Size(9, 9), 2, 2);
        ImShow("Cercle Rouge lissé", threshold_img);
        return threshold_img;

    }

    public static List<MatOfPoint> outlines(Mat threshold_img) {
        int tresh = 100;
        Mat canny_output = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        MatOfInt4 hiearchy = new MatOfInt4();
        Imgproc.Canny(threshold_img, canny_output, tresh, tresh * 2);
        Imgproc.findContours(canny_output, contours, hiearchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat drawing = Mat.zeros(canny_output.size(), CvType.CV_8UC3);
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(255, 255, 255);
            Imgproc.drawContours(drawing, contours, i, color, 1, 8, hiearchy, 0, new Point());
        }
        ImShow("Contours", drawing);
        return contours;
    }

    public static void circlesDetection(List<MatOfPoint> outlines_img, Mat src) {
        Mat cercle = Mat.zeros(src.size(), src.type());
        MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        float[] radius = new float[1];
        Point center = new Point();
        for (int c = 0; c < outlines_img.size(); c++) {
            MatOfPoint outline = outlines_img.get(c);
            double contourAera = Imgproc.contourArea(outline);
            matOfPoint2f.fromList(outline.toList());
            Imgproc.minEnclosingCircle(matOfPoint2f, center, radius);

            if ((contourAera / (Math.PI * radius[0] * radius[0])) >= 0.8) {
                Imgproc.circle(cercle, center, (int) radius[0], new Scalar(0, 255, 0), 2);
            }
        }
        ImShow("detection cercle", cercle);
    }

    public static List<Mat> circlesDetectionExtraction(List<MatOfPoint> outlines_img, Mat src) {
        List<Mat> balls = new ArrayList<>();
        MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        float[] radius = new float[1];
        Point center = new Point();
        for (int c = 0; c < outlines_img.size(); c++) {
            MatOfPoint outline = outlines_img.get(c);
            double contourAera = Imgproc.contourArea(outline);
            matOfPoint2f.fromList(outline.toList());
            Imgproc.minEnclosingCircle(matOfPoint2f, center, radius);
            if ((contourAera / (Math.PI * radius[0] * radius[0])) >= 0.8) {
                Imgproc.circle(src, center, (int) radius[0], new Scalar(0, 255, 0), 2);
                Rect rect = Imgproc.boundingRect(outline);
                Imgproc.rectangle(src, new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(0, 255, 0), 2);
                Mat tmp = src.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width);
                Mat ball = Mat.zeros(tmp.size(), tmp.type());
                tmp.copyTo(ball);
                ImShow("Ball", ball);
                balls.add(ball);
            }
        }
        return balls;
    }

    public static List<Mat> miseALEchelleBalls(List<Mat> balls, Mat sroadSign) {
        List<Mat> ballsEchelle = new ArrayList<>();
        for (int c = 0; c < balls.size(); c++) {
            Mat sObject = new Mat();
            Imgproc.resize(balls.get(c), sObject, sroadSign.size());
            Mat grayObject = new Mat(sObject.rows(), sObject.cols(), sObject.type());
            Imgproc.cvtColor(sObject, grayObject, Imgproc.COLOR_BGRA2GRAY);
            Core.normalize(grayObject, grayObject, 0, 255, Core.NORM_MINMAX);
            ballsEchelle.add(grayObject);
        }
        return ballsEchelle;
    }

    public static Mat miseALEchellesroadSign(List<Mat> balls, Mat sroadSign) {
        Mat sroadSignEchelle = new Mat(sroadSign.rows(), sroadSign.cols(), sroadSign.type());
        Imgproc.cvtColor(sroadSign, sroadSignEchelle, Imgproc.COLOR_BGRA2GRAY);
        Core.normalize(sroadSignEchelle, sroadSignEchelle, 0, 255, Core.NORM_MINMAX);
        return sroadSignEchelle;
    }

    public static void matching(List<Mat> balls, Mat sroadSign) {
        List<Mat> ballsEchelle = miseALEchelleBalls(balls, sroadSign);
        Mat sroadSignEchelle = miseALEchellesroadSign(balls, sroadSign);
        ORB orbDetector = ORB.create();
        ORB orbExtractor = ORB.create();

        MatOfKeyPoint sroadSignKeypoints = new MatOfKeyPoint();
        orbDetector.detect(sroadSignEchelle,sroadSignKeypoints);

        Mat sroadSigntDescriptor = new Mat(sroadSign.rows(), sroadSign.cols(), sroadSign.type());
        orbExtractor.compute(sroadSignEchelle,sroadSignKeypoints,sroadSigntDescriptor);

        for (int c = 0; c < balls.size(); c++) {

            Mat sObject = new Mat();
            Imgproc.resize(balls.get(c), sObject, sroadSign.size());

            MatOfKeyPoint objectKeypoints = new MatOfKeyPoint();
            orbDetector.detect(ballsEchelle.get(c),objectKeypoints);

            Mat objectDescriptor = new Mat(balls.get(c).rows(), balls.get(c).cols(), balls.get(c).type());
            orbExtractor.compute(ballsEchelle.get(c),objectKeypoints,objectDescriptor);

            MatOfDMatch matchs = new MatOfDMatch();
            DescriptorMatcher matcher =DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
            matcher.match(sroadSigntDescriptor,objectDescriptor,matchs);
            System.out.println(matchs.dump());
            System.out.println();
            Mat matchedImage = new Mat(sroadSign.rows(),sroadSign.cols()*2,sroadSign.type());
            Features2d.drawMatches(sroadSign,sroadSignKeypoints,sObject,objectKeypoints,matchs,matchedImage);

            ImShow("Ball", matchedImage);

        }
    }
}
