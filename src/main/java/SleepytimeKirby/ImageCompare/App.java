package SleepytimeKirby.ImageCompare;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import javax.imageio.ImageIO;


public class App 
{
	
	private static final Double VHC = 4181.00;
	private static final Color IGNORED_COLOR = new Color(255,0,90);
	private static HashMap<Color,Double> tempColorMap = new HashMap<Color,Double>();
	
    public static void main( String[] args )
    {
    	
        File imageA = null;
        File imageB = null;
        File tempScale = null;
        if (args.length > 2) {
        	
        		imageA = new File(args[0]);
        		imageB = new File(args[1]);
        		tempScale = new File(args[2]);
        		try {
        			loadTempScale(tempScale);
					compareImage(imageA,imageB);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	
        } else {
        	System.out.println("Error, not enough args.");
        	System.out.println("Requires 3 args. First is Before image, Second is After image, Third is tempertaure scale.");
        }
        
       
    }
    public static void loadTempScale(File tempScaleFile) throws IOException {
    	BufferedImage tempScale = ImageIO.read(tempScaleFile); // Temp scale should start at 0. There should be 30 pixels per 5C.Height should be 1 pixel
    	int[] tempColors = tempScale.getRGB(0, 0, tempScale.getWidth(), 1, null, 0, tempScale.getWidth());
    	//Temp = (5/30) * (current pixel)
    	for(int i = 0; i < tempColors.length; i++) {
    		
    		tempColorMap.put(new Color(tempColors[i]), (5.00/30.00)*(double)i);
    	}
    }
    //Infomration on this can be found https://www.compuphase.com/cmetric.htm
    public static double compareColorDistance(Color a, Color b) {
    	int redA = a.getRed();
    	int redB = b.getRed();
    	int blueA = a.getBlue();
    	int blueB = b.getBlue();
    	int greenA = a.getGreen();
    	int greenB = b.getGreen();
    	long redMean = (redA + redB)/2;
    	long rDelta = redA - redB;
    	long bDelta = blueA - blueB;
    	long gDelta = greenA - greenB;
    	long rDiff = ((512+redMean)*rDelta*rDelta)>>8;
    	long gDiff = 4*gDelta*gDelta;
    	long bDiff = ((767-redMean)*bDelta*bDelta)>>8;
    	double output = Math.sqrt(rDiff+gDiff+bDiff);
    	return output;
    			
    }
    public static Color findClostestColor(Color check,Set<Color> colors) {
    	HashMap<Double,Color> distances = new HashMap<Double,Color>();
    	for(Color color:colors) {
    		double key = compareColorDistance(check,color);
    		distances.put(key, color);
    	}
    	double min = Collections.min(distances.keySet());
    	return distances.get(min);
    }
    public static void compareImage(File imageA , File imageB) throws IOException {

    	//Load Images for comparison
    	BufferedImage imageABuf = ImageIO.read(imageA);
    	BufferedImage imageBBuf = ImageIO.read(imageB);
    	// Get an array of colors from the image. Each int represents the rgb value of the color. The first 8 bits of the int is hte alpha, second 8 is the red value, thirs is green, and fourth is blue.
    	int[] imageAcolors = imageABuf.getRGB(0, 0, imageABuf.getWidth(), imageABuf.getHeight(), null, 0, imageABuf.getWidth());
    	int[] imageBcolors = imageBBuf.getRGB(0, 0, imageBBuf.getWidth(), imageBBuf.getHeight(), null, 0, imageBBuf.getWidth());
    	//Convert the colors to temps
    	double[] imageATemps = new double[imageAcolors.length];
    	double[] imageBTemps = new double[imageBcolors.length];
    	for(int i= 0; i < imageAcolors.length; i++) {
    		Color imageColor = new Color(imageAcolors[i]);
    		if(compareColorDistance(imageColor,IGNORED_COLOR) < 2) {
    			imageATemps[i] = 1000; // we just ignore this number on our comparistons, so it should be something outragous
    		} else {
    			imageATemps[i] = tempColorMap.get(findClostestColor(imageColor,tempColorMap.keySet()));
    		}
    	}
    	for(int i= 0; i < imageBcolors.length; i++) {
    		Color imageColor = new Color(imageBcolors[i]);
    		if(compareColorDistance(imageColor,IGNORED_COLOR) < 2) {
    			imageBTemps[i] = 1000; // we just ignore this number on our comparistons, so it should be something outragous
    		} else {
    			imageBTemps[i] = tempColorMap.get(findClostestColor(imageColor,tempColorMap.keySet()));
    		}
    	}
    	//Get temp difference per pixel (which is 12.5km per pixel) Cooling will be in the negatives, heating is postive values ignore  land
    	double[] deltaT = new double[imageAcolors.length];
    	for (int i = 0; i < imageAcolors.length; i++) {
    		if(imageATemps[i] == 1000 || imageBTemps[i] == 1000) {
    			deltaT[i] = 0;
    		} else {
    			deltaT[i] = imageATemps[i] - imageBTemps[i];
    		}
    	}
    	
    	//Caluclate the heat abosored (disregard any positive numbers
    	//Area per pixel is 156.25km^2, volume is 156200 m^3. VHC = 4.181x10^3 kJ/m^3C or 4181 kJ/m^3C
    	//Heat = VHC * volume * temp dif
    	double[] heats =  new double[imageAcolors.length];
    	for (int i = 0; i < imageAcolors.length; i++) {
    		if(deltaT[i] < 0) {
    		heats[i] = VHC * 156200 * deltaT[i];
    		}
    	}
  
    	//Add everything together
    	double totalHeat = 0;
    	for(double heat:heats) {
    		totalHeat += heat;
    	}
    	//Output
    	System.out.println("Total Heat: " + totalHeat);
    }

}
