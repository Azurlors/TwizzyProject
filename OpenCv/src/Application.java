import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import java.lang.Math;
public class Application extends JFrame{
    private JPanel panelMain;
    private JButton btnImport;
    private JPanel imagePanel;
    private JLabel loadedImage;
    private JButton startButton;
    private JRadioButton templateMatchingRadioButton;
    private JRadioButton machineLearningRadioButton;
    private BufferedImage  img = null;
    private File selectedFile=null;
    private boolean templateMatching = false;
    private boolean machineLearning = false;
    private boolean newImage = false;
    public static final List<String> patternSigns = new ArrayList<>(List.of("ref30.jpg","ref50.jpg","ref70.jpg","ref90.jpg","ref110.jpg","refdouble.jpg"));
    private static final Application h= new Application();
    public Application() {
        btnImport.addActionListener(e -> {
            //selection d'une image depuis l'explorateur de fichier
            JFileChooser fileChooser = new JFileChooser();
            String directoryPath = Paths.get(".").toAbsolutePath().normalize().toString(); // replace with your desired directory path
            int index = directoryPath.lastIndexOf('\\');
            directoryPath = directoryPath.substring(0, index);
            fileChooser.setCurrentDirectory(new File(directoryPath));
            int result = fileChooser.showOpenDialog(h);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                displayImageFromFile();
                newImage=true;
            }
        });
        //afficher image avec rectangle sur le match
        startButton.addActionListener(e -> {
            if (newImage) {
                if (templateMatching) {
                    img = PatternMatching.bestMatches(selectedFile, patternSigns);
                    displayImage();
                } else if (machineLearning) {
                    deleteExpDirectory();
                    yoloDetection();
                    displayImageFromFile();
                }
                newImage = false;
            }
            else{
                JOptionPane.showMessageDialog(h,"Rentrer une nouvelle image");
            }
        });
        templateMatchingRadioButton.addActionListener(e -> {
            templateMatching=true;
            machineLearning=false;
        });
        machineLearningRadioButton.addActionListener(e -> {
            templateMatching=false;
            machineLearning=true;
        });
    }
    private void displayImageFromFile(){
        System.out.println("Selected file: " + selectedFile.getAbsolutePath());
        try{
            img = ImageIO.read(new File(selectedFile.getAbsolutePath()));
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        int height = img.getHeight();
        int width = img.getWidth();
        int newWidth = Math.min(800*width/height,1500);

        Image resultingImage = img.getScaledInstance(newWidth, 800, Image.SCALE_DEFAULT);
        loadedImage.setIcon(new ImageIcon(resultingImage));
    }
    private void displayImage(){

        int height = img.getHeight();
        int width = img.getWidth();
        int newWidth = Math.min(800*width/height,1500);

        Image resultingImage = img.getScaledInstance(newWidth, 800, Image.SCALE_DEFAULT);
        loadedImage.setIcon(new ImageIcon(resultingImage));
    }
    private void createUIComponents() {
        //initialisation avec une image vide (evite erreur null)
        try{
            img = ImageIO.read(new File(Paths.get(".").toAbsolutePath().normalize().toString() + "\\invisible.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        assert img != null;
        loadedImage = new JLabel(new ImageIcon(img));
    }

    private void yoloDetection(){
        String directoryPath = Paths.get(".").toAbsolutePath().normalize().toString(); // replace with your desired directory path
        int index = directoryPath.lastIndexOf('\\');
        directoryPath = directoryPath.substring(0, index);
        directoryPath = directoryPath.substring(0, index);
        directoryPath = directoryPath + "\\neural_net\\yolov5";
        String command = "python detect.py --weights epoch160_batch6.pt --source "+selectedFile.getAbsolutePath(); // replace with your desired command

        try {
            ProcessBuilder builder = new ProcessBuilder(command.split("\\s+"));
            builder.directory(new File(directoryPath));
            Process process = builder.start();

            int exitCode = process.waitFor();
            System.out.println("Command exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        String filePath = Paths.get(".").toAbsolutePath().normalize().toString(); // replace with your desired directory path
        filePath  = filePath .substring(0, index);
        filePath  = filePath .substring(0, index);
        filePath = filePath + "\\neural_net\\yolov5\\runs\\detect\\exp\\"+selectedFile.getName();
        System.out.println("Selected file Yolo: " + filePath);
        selectedFile = new File(filePath);
    }
    private void deleteExpDirectory(){
        String directoryPath =Paths.get(".").toAbsolutePath().normalize().toString(); // replace with your desired directory path
        int index = directoryPath.lastIndexOf('\\');
        directoryPath = directoryPath.substring(0, index);
        directoryPath = directoryPath.substring(0, index);
        directoryPath = directoryPath + "\\neural_net\\yolov5\\runs\\detect\\exp";
        File directory = new File(directoryPath);

        if (directory.exists()) {
            if (directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
            directory.delete();
            System.out.println("Directory deleted successfully.");
        } else {
            System.out.println("Directory does not exist.");
        }
    }

    public static void main(String[] args) {
        h.setContentPane(h.panelMain);
        h.setTitle("Test");
        h.setBounds(0,0,1920,1080);
        h.setResizable(false);
        h.setVisible(true);
        h.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ImageIcon image = new ImageIcon("twizyPNG.png");
        h.setIconImage(image.getImage());
        h.getContentPane().setBackground(new Color(255,255,255));

    }
}