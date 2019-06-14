package Controller;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import DTO.Ball;
import DTO.Camera;
import DTO.Robot;

public class CamController {

	private static JFrame frame;
	private JFrame videoFrame;
	
    private JLabel imgCaptureLabel;
    private JLabel imgDetectionLabel;
	
    private int[][] map = null;
    
	private static List<Ball> balls = new ArrayList<Ball>();
	private static List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	private static List<Ball> triangles = new ArrayList<Ball>();

	static Mat circles;
	Mat imageWithGrid;
	
	private VideoCapture videoCapture;
    private Mat matFrame, realImg;
    private CaptureTask captureTask;
    private Camera cameraSettings;
    private boolean useCam = true;
    private Mat mask, inRange, edges, ballsMask, robotMask;
    private MapController mapController;
    private boolean run = false;
    
    private FrameHelper frameHelper = new FrameHelper();
    
    String imagePath = "Images/findRoboto.jpg";
    
    DTO.Point directionPoint;
    Robot robot;
	
	public CamController(boolean useCam) {
		this.useCam = useCam;
		loadSettingsFile();
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		matFrame = new Mat();

		if(useCam) {
			videoCapture = new VideoCapture(0);
	        if (!videoCapture.isOpened()) {
	            System.err.println("Cannot open camera");
	            System.exit(0);
	        }
	        if (!videoCapture.read(matFrame)) {
	            System.err.println("Cannot read camera stream.");
	            System.exit(0);
	        }
		} else {
			matFrame = Imgcodecs.imread(imagePath);
		}
	}
	
	private void loadSettingsFile() {
		try {
			File file = new File("camera_settings.xml");
			JAXBContext jaxbContext = JAXBContext.newInstance(Camera.class);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			cameraSettings = (Camera) jaxbUnmarshaller.unmarshal(file);
			
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void startUp() {
		videoFrame = new JFrame("Video");
		videoFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame = new JFrame("CamController");
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JButton recalibrateBtn = new JButton("Kalibrer Kamera");
	    JButton useDefaultBtn = new JButton("Brug Standard");
	    
	    recalibrateBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				calibrateCamera();
				frame.setVisible(false);
				frame.dispose();
			}
		});
	    
	    useDefaultBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(useCam) {
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
			            @Override
			            public void run() {
			                captureTask = new CaptureTask();
			                captureTask.execute();
			            }
			        });
				}

				frame.setVisible(false);
				frame.dispose();
				
				Image img = HighGui.toBufferedImage(matFrame);
		        
		        JPanel framePanel = new JPanel();
		        imgCaptureLabel = new JLabel(new ImageIcon(img));
		        framePanel.add(imgCaptureLabel);
		        imgDetectionLabel = new JLabel(new ImageIcon(img));
		        framePanel.add(imgDetectionLabel);
		        videoFrame.getContentPane().add(framePanel, BorderLayout.CENTER);
				updateFrame();
				videoFrame.pack();
				videoFrame.setVisible(true);
			}
		});
	    
	    JPanel panel = new JPanel();
	    panel.add(recalibrateBtn);
	    panel.add(useDefaultBtn);
	    frame.add(panel, BorderLayout.CENTER);
	    frame.pack();
	    frame.setLocationRelativeTo(null);
	    frame.setVisible(true);
	}
	
	private void calibrateCamera() {
        // Set up the content pane.
        Image img = HighGui.toBufferedImage(matFrame);
        
        JPanel framePanel = new JPanel();
        imgCaptureLabel = new JLabel(new ImageIcon(img));
        framePanel.add(imgCaptureLabel);
        imgDetectionLabel = new JLabel(new ImageIcon(img));
        framePanel.add(imgDetectionLabel);
        videoFrame.getContentPane().add(framePanel, BorderLayout.CENTER);
        
        JFrame menu = frameHelper.calibrationMenu();
        
        frameHelper.minBallSize.setValue(cameraSettings.getMinBallSize());
        frameHelper.maxBallSize.setValue(cameraSettings.getMaxBallSize());
        frameHelper.lowHueWalls.setValue(cameraSettings.getLowHueWalls());
        frameHelper.maxHueWalls.setValue(cameraSettings.getMaxHueWalls());
        frameHelper.lowSatWalls.setValue(cameraSettings.getLowSatWalls());
        frameHelper.maxSatWalls.setValue(cameraSettings.getMaxSatWalls());
        frameHelper.lowValWalls.setValue(cameraSettings.getLowValWalls());
        frameHelper.maxValWalls.setValue(cameraSettings.getMaxValWalls());
        frameHelper.lowHueBalls.setValue(cameraSettings.getLowHueBalls());
        frameHelper.maxHueBalls.setValue(cameraSettings.getMaxHueBalls());
        frameHelper.lowSatBalls.setValue(cameraSettings.getLowSatBalls());
        frameHelper.maxSatBalls.setValue(cameraSettings.getMaxSatBalls());
        frameHelper.lowValBalls.setValue(cameraSettings.getLowValBalls());
        frameHelper.maxValBalls.setValue(cameraSettings.getMaxValBalls());
        updateFrame();

        ChangeListener changeListener = new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				cameraSettings.setMinBallSize(frameHelper.minBallSize.getValue());
				cameraSettings.setMaxBallSize(frameHelper.maxBallSize.getValue());
				cameraSettings.setLowHueWalls(frameHelper.lowHueWalls.getValue());
				cameraSettings.setMaxHueWalls(frameHelper.maxHueWalls.getValue());
				cameraSettings.setLowSatWalls(frameHelper.lowSatWalls.getValue());
				cameraSettings.setMaxSatWalls(frameHelper.maxSatWalls.getValue());
				cameraSettings.setLowValWalls(frameHelper.lowValWalls.getValue());
				cameraSettings.setMaxValWalls(frameHelper.maxValWalls.getValue());
				cameraSettings.setLowHueBalls(frameHelper.lowHueBalls.getValue());
				cameraSettings.setMaxHueBalls(frameHelper.maxHueBalls.getValue());
				cameraSettings.setLowSatBalls(frameHelper.lowSatBalls.getValue());
				cameraSettings.setMaxSatBalls(frameHelper.maxSatBalls.getValue());
				cameraSettings.setLowValBalls(frameHelper.lowValBalls.getValue());
				cameraSettings.setMaxValBalls(frameHelper.maxValBalls.getValue());
				
				if(!useCam) {
					matFrame = Imgcodecs.imread(imagePath);
				}
				
				updateFrame();
			}
			
		};
		
		frameHelper.setListener(changeListener);
        
        frameHelper.save.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Camera camera = new Camera(frameHelper.minBallSize.getValue(), frameHelper.maxBallSize.getValue(), frameHelper.lowHueWalls.getValue(), frameHelper.maxHueWalls.getValue(), frameHelper.lowSatWalls.getValue(), frameHelper.maxSatWalls.getValue(), frameHelper.lowValWalls.getValue(), frameHelper.maxValWalls.getValue(), frameHelper.lowHueBalls.getValue(), frameHelper.maxHueBalls.getValue(), frameHelper.lowSatBalls.getValue(), frameHelper.maxSatBalls.getValue(), frameHelper.lowValBalls.getValue(), frameHelper.maxValBalls.getValue());
				
				try {
					File file = new File("camera_settings.xml");
					JAXBContext jaxbContext = JAXBContext.newInstance(Camera.class);
					Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

					// output pretty printed
					jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

					jaxbMarshaller.marshal(camera, file);
					jaxbMarshaller.marshal(camera, System.out);
					run = true;
				} catch (JAXBException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
        
        menu.setVisible(true);
        
        videoFrame.pack();
        videoFrame.setLocationRelativeTo(null);
        videoFrame.setVisible(true);
        
		if(useCam) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
	            @Override
	            public void run() {
	                captureTask = new CaptureTask();
	                captureTask.execute();
	            }
	        });
		}
	}
	
	private void updateFrame() {
		realImg = matFrame.clone();
		Mat capturedFrame = matFrame.clone();
		mask = new Mat();
		Mat threshold = new Mat();
		edges = new Mat();
		
		Imgproc.blur(capturedFrame, capturedFrame, new Size(7,7));
		
		Imgproc.cvtColor(capturedFrame, mask, Imgproc.COLOR_BGR2HSV);
		
		//Imgproc.adaptiveThreshold(mask, threshold, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

		inRange = new Mat();
		
		Scalar lower = new Scalar(cameraSettings.getLowHueWalls(), cameraSettings.getLowSatWalls(), cameraSettings.getLowValWalls());
	    Scalar upper = new Scalar(cameraSettings.getMaxHueWalls(), cameraSettings.getMaxSatWalls(), cameraSettings.getMaxValWalls());
	    
        Core.inRange(mask, lower, upper, inRange);
                
        int lowThresh = 90;

		Imgproc.Canny(inRange, edges, lowThresh, lowThresh*3, 3, true);
		

		List<MatOfPoint> contoursWalls = new ArrayList<MatOfPoint>();

		Imgproc.findContours(edges, contoursWalls, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

		double areaLast = 0;
		double crossArea = 0.0;
		int crossI = -1;
		MatOfPoint crossContour = null;
		Point[] verticesLast = null;
		RotatedRect rectLast = null;
		
		for (int i = 0; i < contoursWalls.size(); i++) {
			MatOfPoint2f temp = new MatOfPoint2f(contoursWalls.get(i).toArray());
			MatOfPoint2f approxCurve = new MatOfPoint2f();
			Imgproc.approxPolyDP(temp, approxCurve, Imgproc.arcLength(temp, true) * 0.004, true);
			
			if(approxCurve.total() == 4) {
				RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contoursWalls.get(i).toArray()));
				Point[] vertices = new Point[4];  
		        rect.points(vertices);
		        
				double area = rect.size.width * rect.size.height;
				
				if(area > areaLast) {
		        	verticesLast = vertices;
			        rectLast = rect;
					areaLast = area;
				}
			}else {
				double crossAreaLocal = Imgproc.contourArea(approxCurve);
				if(crossAreaLocal > crossArea && crossAreaLocal >= (int)frameHelper.minCrossArea.getValue() && crossAreaLocal <= (int)frameHelper.maxCrossArea.getValue()) {
					crossArea = crossAreaLocal;
					crossI = i;
					crossContour = contoursWalls.get(i);
				}
				
			}
		}
		
		
		
		if(verticesLast != null && rectLast != null) {
			for(int j = 0; j < 4; j++) {
				Imgproc.line(matFrame, verticesLast[j], verticesLast[(j+1)%4], new Scalar(0,255,0));
				Imgproc.putText(matFrame, verticesLast[j] + "", verticesLast[j], 2, 0.5, new Scalar(250,250,250));
			}
		}
		
		Mat src_mat=new Mat(4,1,CvType.CV_32FC2);
	    Mat dst_mat=new Mat(4,1,CvType.CV_32FC2);

	    src_mat.put(0, 0, verticesLast[2].x, verticesLast[2].y, verticesLast[3].x, verticesLast[3].y, verticesLast[1].x, verticesLast[1].y, verticesLast[0].x, verticesLast[0].y);
	    dst_mat.put(0, 0, 0.0, 0.0, rectLast.size.height, 0.0, 0.0, rectLast.size.width, rectLast.size.height, rectLast.size.width);
	    Mat perspectiveTransform = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

	    Imgproc.warpPerspective(matFrame, matFrame, perspectiveTransform, new Size(rectLast.size.height, rectLast.size.width));
		
		findBalls(matFrame);
		findRobot(matFrame);
		
		if(crossI > 0) {
			//Imgproc.drawContours(matFrame, contoursWalls, crossI, new Scalar(255,0,0), Imgproc.FILLED);
			
			if(crossContour != null) {
				//TODO: FIX DIS SHIT
				System.out.println(crossContour);
				List<Point> pointsContour = crossContour.toList();
			
		        Collections.sort(pointsContour, new SortCoordinates());
		        
		        List<Point> cross = new ArrayList<Point>();
		        
		        Point topL = null, bottomL = null, leftL = null, rightL = null;

		        Point topR = null, bottomR = null, leftR = null, rightR = null;
		        
		        double gridSizeHorizontal = matFrame.width()/180;
		        double gridSizeVertical = matFrame.height()/120;
		        
		        map = new int[180][120];
		        
		        for (int i = 0; i < 180; i++) {
					for (int j = 0; j < 120; j++) {
						map[i][j] = 0;
					}
				}
		        

	        	double gridSizeHorizontald = matFrame.width()/180;
	            double gridSizeVerticald = matFrame.height()/120;
		        
		        for(Point point : pointsContour) {
		        	/*
		        	if(topL == null) {
		        		topL = point;
		        		bottomL = point;
		        		leftL = point;
		        		rightL = point;
		        		topR = point;
		        		bottomR = point;
		        		leftR = point;
		        		rightR = point;
		        	} else {
		        		if(leftL.x > point.x) {
		        			leftL = point;
		        		}
		        		
		        		if(rightL.x < point.x) {
		        			rightL = point;
		        		} else if(rightR.x < point.x || point.y > rightL.y) {
		        			rightR = point;
		        		}
		        		
		        		if(point.y < topL.y) {
		        			topL = point;
		        		} else if(point.y <= topR.y || (point.x > topL.x && point.y <= topL.y)) {
		        			topR = point;
		        		}
		        		
		        		if(point.y >= bottomL.y && point.x >= bottomL.x) {
		        			bottomL = point;
		        		} else if(point.y >= bottomR.y && point.x < bottomL.x) {
		        			bottomR = point;
		        		}
		        	}
		        	*/
		            
		    		int botX = (int) Math.round(point.x/gridSizeHorizontal);
		    		int botY = (int) Math.round(point.y/gridSizeVertical);
		    		map[botX][botY] = 1;
		        }
		        
		        for(int i = 0; i < 180; i++) {
					for(int j = 0; j < 120; j++) {
						System.out.print(map[i][j]);
					}
					System.out.println();
				}

		        /*
		        System.out.println(rightL + " " + rightR);

		        Imgproc.circle(matFrame, bottomL, 3, new Scalar(0,255, 0), Imgproc.FILLED);
		        Imgproc.circle(matFrame, bottomR, 3, new Scalar(0,255, 0), Imgproc.FILLED);
		        
		        Imgproc.circle(matFrame, leftL, 3, new Scalar(0,255, 0), Imgproc.FILLED);

		        Imgproc.circle(matFrame, leftR, 3, new Scalar(0,255, 0), Imgproc.FILLED);
		        
		        Imgproc.circle(matFrame, topL, 3, new Scalar(0,255, 0), Imgproc.FILLED);
		        Imgproc.circle(matFrame, topR, 3, new Scalar(0,255, 0), Imgproc.FILLED);
		        Imgproc.circle(matFrame, rightL, 3, new Scalar(0,255, 0), Imgproc.FILLED);
		        Imgproc.circle(matFrame, rightR, 3, new Scalar(0,255, 0), Imgproc.FILLED);
				*/
		        	
		        
			}
		}
		

		if(robot != null && directionPoint != null) {
			mapController.robot = robot;
			Imgproc.circle(matFrame, new Point(robot.x, robot.y), 2, new Scalar(0,0,255), Imgproc.FILLED);

			Imgproc.line(matFrame, new Point(robot.x, robot.y), new Point(directionPoint.x, directionPoint.y), new Scalar(0,0,255));
		}
		
		
		if(areaLast > 0 && crossArea > 0) {
			if(mapController.isReady() && run) {
				generateMap(realImg);
				if(getMap() != null)
					try {
						mapController.loadMap(getMap());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		}
		
		
        imgCaptureLabel.setIcon(new ImageIcon(HighGui.toBufferedImage(matFrame)));
        imgDetectionLabel.setIcon(new ImageIcon(HighGui.toBufferedImage(edges)));
        videoFrame.repaint();
	}
	
	private void findRobot(Mat matFrame) {
		Mat matFrameCopy = matFrame.clone();
		
		robotMask = new Mat();
		
		triangles.clear();

		Imgproc.blur(matFrameCopy, matFrameCopy, new Size(7,7));
		
		List<MatOfPoint> contoursRoboto = new ArrayList<MatOfPoint>();
				
		Imgproc.cvtColor(matFrameCopy, robotMask, Imgproc.COLOR_BGR2GRAY);
		
		Scalar lower = new Scalar(cameraSettings.getLowHueBalls(), cameraSettings.getLowSatBalls(), cameraSettings.getLowValBalls());
	    Scalar upper = new Scalar(cameraSettings.getMaxHueBalls(), cameraSettings.getMaxSatBalls(), cameraSettings.getMaxValBalls());
	    
        Core.inRange(robotMask, lower, upper, robotMask);

		Mat canny = new Mat();
		
		int lowThresh = 100;
		
        Imgproc.Canny(robotMask, canny, lowThresh, lowThresh * 3, 3, false);
		Imgproc.findContours(canny, contoursRoboto, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

		MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
		MatOfPoint2f approxCurve = new MatOfPoint2f();
		
		Imgproc.drawContours(matFrame, contoursRoboto, -1, new Scalar(255,0,0));
		
		double areaLast = 0;
		MatOfPoint contourLast = null;
		int contourI = -1;
		
		for (int i = 0; i < contoursRoboto.size(); i++) {
			MatOfPoint contour = contoursRoboto.get(i);
		    double contourArea = Imgproc.contourArea(contour);
		    
		    if(contourArea > areaLast && contourArea > 400) {
		    	areaLast = contourArea;
		    	contourLast = contour;
		    	contourI = i;
			}
		}

		if(contourI != -1) {
		
			List<Point> pointsContour = contourLast.toList();
		
	        Collections.sort(pointsContour, new SortCoordinates());
	        
	        List<Point> triangle = new ArrayList<Point>();
	        
	        Point top = null, left = null, right = null;
	        
	        boolean upward = true;

	        for(Point point : pointsContour) {
	        	if(top == null) {
	        		top = point;
	        		left = point;
	        		right = point;
	        	} else {
	        		if(left.x > point.x) {
	        			left = point;
	        		} else if(right.x < point.x) {
	        			right = point;
	        		} else if(point.y < top.y) {
	        			top = point;
	        		}
	        	}
	        }

	        if(left.y-50 > top.y || right.y-50 > top.y) {
        		upward = true;
        	} else {
        		upward = false;
        	}
	        
	        for(Point point : pointsContour) {
	        	
	        	if(upward) {
	        		if(point.y < top.y) {
	        			top = point;
	        		}
	        	} else {
	        		if(point.y > top.y) {
	        			top = point;
	        		}
	        	}
	        }
	        
	        Imgproc.circle(matFrame, left, 3, new Scalar(0,0, 255), Imgproc.FILLED);
	        Imgproc.circle(matFrame, right, 3, new Scalar(0,0, 255), Imgproc.FILLED);
	        Imgproc.circle(matFrame, top, 3, new Scalar(0,0, 255), Imgproc.FILLED);
	        
	        double distTopRight = Math.hypot(top.x-right.x, top.y-right.y);
	        double distTopLeft = Math.hypot(top.x-left.x, top.y-left.y);
	        double distLeftRight = Math.hypot(left.x-right.x, left.y-right.y);
	        
	        if(distTopRight > 30 && distTopLeft > 30 && distLeftRight > 30) {
	        	double sumTop = distTopLeft + distTopRight;
	        	double sumLeft = distTopLeft + distLeftRight;
	        	double sumRight = distLeftRight + distTopRight;
	        	
	        	if(sumTop > sumLeft && sumTop > sumRight) {
	        		directionPoint = new DTO.Point(top.x, top.y);
	        	} else if(sumLeft > sumTop && sumLeft > sumRight) {
	        		directionPoint = new DTO.Point(left.x, left.y);
	        	} else if(sumRight > sumTop && sumRight > sumLeft) {
	        		directionPoint = new DTO.Point(right.x, right.y);
	        	}
	        	
	        	robot = new Robot((top.x+right.x+left.x)/3, (top.y+right.y+left.y)/3);
	        	
	        }
		}
		
	}
	
	class SortCoordinates implements Comparator<Point>{

		@Override
		public int compare(Point point1, Point point2) {
			int result = Double.compare(point1.x, point2.x);
	         if ( result == 0 ) {
	           // both X are equal -> compare Y too
	           result = Double.compare(point1.y, point2.y);
	         } 
	         return result;
		}
		
	}
	
	private void findBalls(Mat matFrame) {
		Mat matFrameCopy = matFrame.clone();
		Mat mask = new Mat();
		ballsMask = new Mat();
		

		Imgproc.blur(matFrameCopy, matFrameCopy, new Size(3,3));
		
		Imgproc.cvtColor(matFrameCopy, ballsMask, Imgproc.COLOR_BGR2GRAY);
		
		Scalar lower = new Scalar(cameraSettings.getLowHueBalls(), cameraSettings.getLowSatBalls(), cameraSettings.getLowValBalls());
	    Scalar upper = new Scalar(cameraSettings.getMaxHueBalls(), cameraSettings.getMaxSatBalls(), cameraSettings.getMaxValBalls());
	    
        Core.inRange(ballsMask, lower, upper, ballsMask);

		Mat canny = new Mat();

		contours.clear();
		balls.clear();
		
		int lowThresh = 100;
		
        Imgproc.Canny(ballsMask, canny, lowThresh, lowThresh * 3, 3, false);
		
		Imgproc.findContours(canny, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

		double minArea = Math.PI * (cameraSettings.getMinBallSize() * 0.9f) * (cameraSettings.getMinBallSize() * 0.9f); // minimal ball area
		double maxArea = Math.PI * (cameraSettings.getMaxBallSize() * 1.1f) * (cameraSettings.getMaxBallSize() * 1.1f); // maximal ball area
		
		

		for (int i = 0; i < contours.size(); i++) {
			
			
			double area = Imgproc.contourArea(contours.get(i));
			
			if (area > minArea) {		
				if (area < maxArea) {
					// we found a ball
					
					float[] radius = new float[1];
					Point center = new Point();
					Imgproc.minEnclosingCircle(new MatOfPoint2f(contours.get(i).toArray()), center, radius);
					
					boolean contains = false;
					
					for (int j = 0; j < balls.size(); j++) {
						if(balls.get(j).checkCoords((int)Math.round(center.x), (int)Math.round(center.y)) && contains == false) {
							contains = true;
							break;
						}
					}
					
					if(contains == false)
						balls.add(new Ball((int)Math.round(center.x), (int)Math.round(center.y)));
				}
			}
		}
		
		for (Ball ball : balls) {
			Imgproc.circle(matFrame, new Point(ball.x, ball.y), 20, new Scalar(0, 0, 255));
			Imgproc.putText(matFrame, "bold", new Point(ball.x, ball.y-20), 3, 1.5, new Scalar(0, 0, 255));
		}
		
	}
	
	
	public int[][] getMap(){
		return map;
	}
	
	private void generateMap(Mat img) {
		double gridSizeHorizontal = img.width()/180;
        double gridSizeVertical = img.height()/120;
        
        map = new int[180][120];
        
        for (int i = 0; i < 180; i++) {
			for (int j = 0; j < 120; j++) {
				map[i][j] = 0;
			}
		}
        
		ArrayList<Ball> notFound = new ArrayList<Ball>();
		
		int botX = (int) Math.round(robot.x/gridSizeHorizontal);
		int botY = (int) Math.round(robot.y/gridSizeVertical);
		
		int directionX = (int) Math.round(directionPoint.x/gridSizeHorizontal);
		int directionY = (int) Math.round(directionPoint.y/gridSizeVertical);
		
		map[botX][botY] = 9;
		map[directionX][directionY] = 3;
		
		for(Ball ball : balls) {
			int i = (int) Math.round(ball.x/gridSizeHorizontal);
			int j = (int) Math.round(ball.y/gridSizeVertical);
			
			if(i < 3) {
				i = 3;
			}
			if(i > 179) {
				i = 179;
			}
			if(j < 2) {
				j = 2;
			}
			if(j > 118) {
				j = 118;
			}
			map[i-3][j-2] = 1;
			map[i-3][j-1] = 1;
			map[i-3][j] = 1;
			map[i-3][j+1] = 1;
			
			map[i-2][j-2] = 1;
			map[i-2][j-1] = 1;
			map[i-2][j] = 1;
			map[i-2][j+1] = 1;
			
			map[i-1][j-2] = 1;
			map[i-1][j-1] = 1;
			map[i-1][j] = 1;
			map[i-1][j+1] = 1;
			
			map[i][j-2] = 1;
			map[i][j-1] = 1;
			map[i][j] = 1;
			map[i][j+1] = 1;

		}
		for(Ball ball : notFound) {
			System.out.println("i: " + ball.x/gridSizeHorizontal + " y: " + ball.y/gridSizeVertical);
		}
		
		int counter = 0;
		for(int i = 0; i < 180; i++) {
			for(int j = 0; j < 120; j++) {
				if(map[i][j] == 1) {
					counter++;
				}
			}
		}
	}
	
	private static void showImage(Mat mat) {
		JFrame f = new JFrame();
		f.setTitle(mat + "");
		f.add(new JPanel().add(new JLabel(new ImageIcon(HighGui.toBufferedImage(mat)))));
		f.setSize((int)mat.size().width, (int)mat.size().height+50);
		f.setVisible(true);

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public static Image toBufferedImage(Mat m){
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if ( m.channels() > 1 ) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte [] b = new byte[bufferSize];
        m.get(0,0,b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);  
        return image;

    }
	
	public MapController getMapController() {
		return mapController;
	}

	public void setMapController(MapController mapController) {
		this.mapController = mapController;
	}



	private class CaptureTask extends SwingWorker<Void, Mat> {
        @Override
        protected Void doInBackground() {
            Mat matFrame = new Mat();
            while (!isCancelled()) {
                if (!videoCapture.read(matFrame)) {
                    break;
                }
                publish(matFrame.clone());
            }
            return null;
        }
        @Override
        protected void process(List<Mat> frames) {
            Mat imgCapture = frames.get(frames.size() - 1);
            matFrame = imgCapture;

            //Findwalls etc her
            updateFrame();
            
            imgCaptureLabel.setIcon(new ImageIcon(HighGui.toBufferedImage(matFrame)));
            imgDetectionLabel.setIcon(new ImageIcon(HighGui.toBufferedImage(edges)));
            videoFrame.repaint();
            
        }
    }
	
}

