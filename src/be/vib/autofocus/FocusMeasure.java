package be.vib.autofocus;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Measurements;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;


/**<p>
 * The <code>FocusMeasure</code> class is a class that provides methods
 * for computing Degree Of Focus of an image.</p>
 * 
 * <p>
 * This class is a <code>Java</code> port of the <code>MATLAB</code> code 
 * <a href="http://www.mathworks.com/matlabcentral/fileexchange/27314-focus-measure/content//fmeasure/fmeasure.m">fmeasure.m</a>
 * </p>
 * 
 * <b>Methods:</b>
 * <p>Multiple method are offered to compute this Degree of Focus.</p>
 * <p><a name="degree_of_focus_method">Degree of Focus Methods</a>:
 * <ul>
 * <li>Tenegrad</li>
 * <li>Tenegrad variance</li>
 * <li>Vollath</li>
 * </ul>
 * </p>
 * 
 * <b>Publication related to best Focus algorithm for Brightfield Images:</b>
 * <ul>
 * <li>On the extended depth of focus algorithms for bright field microscopy, Valdecasas et al, 2001, Micron</li>
 * <li>Automated focusing in bright-field microscopy for tuberculosis detection, Osibote et al, 2010, J Microsc</li>
 * </ul>
 *
 */

public class FocusMeasure implements PlugIn  {
  
  /** plugin's name */
  public static final String PLUGIN_NAME = "Focus Measure";
  public static final String TITLE = "Focus Measure";
  /** plugin's current version */
  public static final String PLUGIN_VERSION = "v0.1";
  
  public static final int TENENGRAD = 0;
  public static final int TENENGRAD_VARIANCE = 1;
  public static final int VOLLATH = 2;
  public static final String[] METHODS = {"Tenengrad", "Tenengrad Variance", "Vollath"}; 
  
  public void run(String arg) {    
    ImagePlus imp = WindowManager.getCurrentImage();
    ImageProcessor ip = imp.getProcessor();    
    double df = getDegreeOfFocusUsingVollath(ip);
    System.out.println(df);
    
////  I1 = Image;
////  I1(1:end-1,:) = Image(2:end,:);
//    ImageProcessor ip1 = ip.duplicate();
//    int[] data = new int[ip1.getWidth()];
//    for(int i=0; i<ip1.getHeight()-1; i++)
//    {
//      ip.getRow(0, i+1, data, ip1.getWidth());
//      ip1.putRow(0, i, data, ip1.getWidth());
//    }
//    //ImagePlus imp1 = new ImagePlus("Result",ip1);
//    //imp1.show();
////  I2 = Image;
////  I2(1:end-2,:) = Image(3:end,:);   
//    ImageProcessor ip2 = ip.duplicate();
//    data = new int[ip2.getWidth()];
//    for(int i=0; i<ip2.getHeight()-2; i++)
//    {
//      ip.getRow(0, i+2, data, ip2.getWidth());
//      ip2.putRow(0, i, data, ip2.getWidth());
//    }
//    //ImagePlus imp2 = new ImagePlus("Result 2",ip2);
//    //imp2.show();
////  Image = Image.*(I1-I2);
////  FM = mean2(Image);
//    FloatProcessor finalProcessor = ip.duplicate().convertToFloatProcessor();
//    for (int x=0; x<ip.getWidth(); x++)
//    {
//      for (int y=0; y<ip.getHeight(); y++)
//      {
//        float currentValue = finalProcessor.getf(x, y);
//        finalProcessor.setf(x, y, currentValue*((float)ip1.getPixel(x, y)-(float)ip2.getPixel(x, y)));
//      }
//    }
//    //ImagePlus imp3 = new ImagePlus("Result 3",finalProcessor);
//    //imp3.show();
////  FM = mean2(Image);
//    double focusMean = mean(finalProcessor);    
//    System.out.println(focusMean);
  }
  
  
  /** Compute a relative Degree of Focus value using the selected method
   * @param ip the image Processor
   * @param method the method used to compute the Degree of Focus. Could be one of
   * {@link #TENENGRAD} or {@link #TENENGRAD_VARIANCE}. or {@link #VOLLATH}
   *  
   * @see #TENENGRAD
   * @see #TENENGRAD_VARIANCE
   * @see #VOLLATH
   * 
   * @return the relative degree of focus
   */
  public static double getDegreeofFocus(ImageProcessor ip, int method)
  {
    double result = -1;
    switch (method) {
    case TENENGRAD:
      result = getDegreeOfFocusUsingTenengrad(ip);
      break;
    case TENENGRAD_VARIANCE:
      result = getDegreeOfFocusUsingTenengrad(ip);
      break;
    case VOLLATH:
      result = getDegreeOfFocusUsingVollath(ip);
      break;
    default:
      break;
    }
    return result;
  }
  
  

  
  /** Compute a relative Degree of Focus value using Vollath's correlation (Santos97)
   * 
   * @param ip the image Processor
   * @return the relative degree of focus
   */
  public static double getDegreeOfFocusUsingVollath(ImageProcessor ip)
  {
    ImageProcessor ip1 = ip.crop();
    int[] data = new int[ip1.getWidth()];
    for(int i=0; i<ip1.getHeight()-1; i++)
    {
      ip.getRow(0, i+1, data, ip1.getWidth());
      ip1.putRow(0, i, data, ip1.getWidth());
    }
    ImageProcessor ip2 = ip.duplicate();
    data = new int[ip2.getWidth()];
    for(int i=0; i<ip2.getHeight()-2; i++)
    {
      ip.getRow(0, i+2, data, ip2.getWidth());
      ip2.putRow(0, i, data, ip2.getWidth());
    }
    FloatProcessor finalProcessor = ip.duplicate().convertToFloatProcessor();
    for (int x=0; x<ip.getWidth(); x++)
    {
      for (int y=0; y<ip.getHeight(); y++)
      {
        float currentValue = finalProcessor.getf(x, y);
        finalProcessor.setf(x, y, currentValue*((float)ip1.getPixel(x, y)-(float)ip2.getPixel(x, y)));
      }
    }
    //return mean(finalProcessor);
    return ImageStatistics.getStatistics(finalProcessor, Measurements.MEAN, null).mean;
  }

  /** Compute a relative Degree of Focus value using  Tenengrad method (Krotkov86)
   * 
   * @param ip the image Processor
   * @return the relative degree of focus
   */
  public static double getDegreeOfFocusUsingTenengrad(ImageProcessor ip)
  {
    ip=ip.crop();
    ip.findEdges();
    return ImageStatistics.getStatistics(ip, Measurements.MEAN, null).mean; 
  }
  
  /** Compute a relative Degree of Focus value using Tenengrad variance (Pech2000)
   * 
   * @param ip the image Processor
   * @return the relative degree of focus
   */
  public static double getDegreeOfFocusUsingTenengradVariance(ImageProcessor ip)
  {
    ip=ip.crop();
    ip.findEdges();
    return ImageStatistics.getStatistics(ip, Measurements.STD_DEV, null).mean; 
  }
  
//  /**
//   * Find the mean value of a Float Processor
//   * @param array : double array 
//   * @return the mean value
//   */
//  public static double mean(FloatProcessor ip) {
//    double sum=0;
//    
//    float[] pixels = (float[])ip.getPixels();
//    for(int i=0; i<pixels.length; i++)
//      sum += pixels[i];
//    
//    return sum / pixels.length;
//  }
  
  
  
  /**
   * Main method for debugging.
   *
   * For debugging, it is convenient to have a method that starts ImageJ, loads an
   * image and calls the plugin, e.g. after setting breakpoints.
   *
   * @param args unused
   */
  public static void main(String[] args) {
    // set the plugins.dir property to make the plugin appear in the Plugins menu
    Class<?> clazz = FocusMeasure.class;
    String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
    String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
    System.setProperty("plugins.dir", pluginsDir);

    // start ImageJ
    new ImageJ();
    ImagePlus image = IJ.openImage("C:\\Users\\u0094799\\Documents\\Work\\GENT\\Code\\Spinning_Disk\\1.tif");
    image.show();

    // run the plugin
    IJ.runPlugIn(clazz.getName(), "");
  }

}
