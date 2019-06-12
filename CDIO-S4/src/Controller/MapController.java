package Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import DTO.Ball;
import DTO.DeliveryPoint;
import DTO.Direction;
import DTO.Point;
import DTO.Robot;

public class MapController {

	List<Ball> balls = new ArrayList<Ball>();
	List<DeliveryPoint> deliveryPoints = new ArrayList<DeliveryPoint>();

	List<Ball> shortestPath = new ArrayList<Ball>();

	ArrayList<Ball> coordinates = new ArrayList<Ball>();

	Robot robot;
	
	Direction direction;
	
	public void findBalls(int[][] map) {
		//int ballcounter;
		boolean ballStatus = false;


		for (int x = 0; x < map.length; x++) { //r�kkerne
			for (int y = 0; y < map[x].length; y++) { //kolonnerne
				if(map[x][y] == 9) {
					robot = new Robot(x,y);
				}
				if(map[x][y] == 3) {
					direction = new Direction(x,y);
				}
				if(map[x][y] == 1) {
					if(map[x+3][y] == 1 && map[x][y+3] == 1 && map[x+3][y+3] == 1) {

						ballStatus = true;

						map[x][y] = 9;
						map[x+3][y] = 0;
						map[x][y+3] = 0;
						map[x+3][y+3] = 0;

						coordinates.add(new Ball(x,y));
					}
				}
			}	
		}
		findShortestPath();	
	}
	
	int[][] map = null;
	public MapController(int[][] loadMap) {
		//det her array er nu bestemt ud fra billedet fra opencv.
		map = loadMap;
		
		System.out.println("int[][] map = new int[][]{");
		/*for (int i = 0; i < 180; i++) {
			System.out.print("{ ");
			for (int j = 0; j < 120; j++) {
				//System.out.print(map[i][j] + "");
				if(j+1 != 120) {
					System.out.print(", ");
				}
			}
			System.out.print(" }");
			if(i+1 != 180) {
				System.out.print(",");
			}
			System.out.println();
		}
		System.out.println("};");*/
	}
	
	public MapController() {
		// TODO Auto-generated constructor stub
	}

	/*
	private void locateBalls() {
		//indsæt dem i balls arraylist
	}*/
	

	public int[][] locateDeliveryPoints(int[][] map) {
		//System.out.println(coordinates);
		return map; 
	}

	public void findShortestPath() {
		Collections.sort(coordinates, new Sort());
		//System.out.println(coordinates);
		Ball ball;
		
		//Iterator<Ball> iterator = coordinates.iterator();
		
		int iterator = coordinates.size();
		
		for(int i = 0; i < iterator; i++) {
			Collections.sort(coordinates, new Sort());
			double[] test = coordinates.get(0).getCoordinates();
			System.out.println("for slut"+ coordinates.size());
			
			for(int j = 0; j < coordinates.size(); j++) {
				//System.out.println("retningspunkt" + direction);
				System.out.println(robot);
				System.out.println(coordinates.get(j)+ " dist = " + robot.dist(coordinates.get(j)) + " angle = " + robot.anglebetween(direction, coordinates.get(j)));
				}
			System.out.println("Antal bolde der mangler at blive besøgt: "+ coordinates.size());

			robot.setCoordinates(test[0], test[1]);
			
			
			//System.out.println(robot + " -> " + ball + " dist = " + robot.dist(ball));
			
			coordinates.remove(0);
			System.out.println("for slut"+ coordinates.size());
			
			
			//while(iterator.hasNext()) {
			/*ball = iterator.next();
			
			coordinates.remove(ball);
			robot.setCoordinates(ball.getCoordinates()[0], ball.getCoordinates()[1]);
			Collections.sort(coordinates, new Sort());
			//Collections.sort(coordinates, new Sort());
			//System.out.println(coordinates);
			iterator = coordinates.iterator();*/
		}
		
	}
	

	class Sort implements Comparator<Point>{

		@Override
		public int compare(Point point1, Point point2) { //skal sorteres p� den rigtige m�de
			//System.out.println(point1 + " " + point2 + " " + (robot.dist(point1) - robot.dist(point2)));
			
			if(robot.dist(point1) < robot.dist(point2)) {
				return -1;
			}
			if(robot.dist(point2) < robot.dist(point1)) {
				return 1;
			}
			
			return 0;
		}

	}

}
