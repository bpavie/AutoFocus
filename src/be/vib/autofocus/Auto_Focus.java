package be.vib.autofocus;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.Measurements;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Choice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

/**
 * @author Benjamin Pavie
 *
 */
//See http://rsb.info.nih.gov/ij/developer/source/ij/plugin/frame/ContrastAdjuster.java.html for a better interface
public class Auto_Focus implements PlugIn, DialogListener
{

  /** Plugin's name */
  public static final String PLUGIN_NAME = "AutoFocus Widefield";
  public static final String TITLE = "Autofocus";
  /** Plugin's current version */
  public static final String PLUGIN_VERSION = "v0.1";
  
  /* (non-Javadoc)
   * @see ij.plugin.PlugIn#run(java.lang.String)
   */
  @Override
  public void run(String arg)
  {
    showDialog();
  }  
  
  private void showDialog()
  {
    int brightfieldChannel = -1;
    ImagePlus imp = IJ.getImage();
    int channelNr = imp.getNChannels();
    final GenericDialog gd = new GenericDialog("Parameters");
    String[] channelArray = new String[channelNr];
    
    for(int i=0;i<channelNr;i++)
      channelArray[i]=(i+1)+"";
    gd.addChoice("Select BrigthField Channel: ", channelArray,"1");
    gd.addChoice("Best Focus Method : ", FocusMeasure.METHODS, FocusMeasure.METHODS[2]);
    gd.addChoice("Projection type: ", ZProjector.METHODS, ZProjector.METHODS[1]);
    gd.setInsets(0,0,5);
    gd.addMessage("Select the Fluorescent Channel you want to process: ");
    gd.setInsets(0,20,0);
    for(int i=0;i<channelArray.length;i++)
      gd.addCheckbox(channelArray[i], false);
    ((Checkbox)gd.getCheckboxes().get(0)).setEnabled(false);
    gd.addDialogListener(this);
    gd.showDialog();
    if (gd.wasCanceled())
      return;
    else
    {
//      String channelStr = gd.getNextChoice();
//      brightfieldChannel = Arrays.asList(channelArray).indexOf(channelStr)+1;
      brightfieldChannel = gd.getNextChoiceIndex()+1;
      @SuppressWarnings("unchecked")
      Vector<Checkbox> checkBoxVector = (Vector<Checkbox>) gd.getCheckboxes();
      ArrayList<Integer> fluorescentChannelArrayList = new ArrayList<Integer>();
      for (int i=0; i<checkBoxVector.size(); i++)
      {
        if(checkBoxVector.get(i).getState())
          fluorescentChannelArrayList.add(i+1);
      }
      int degreeofFocusMethod = gd.getNextChoiceIndex();
      int projectionMethod = gd.getNextChoiceIndex();
      generateMIPBrightFieldStack(imp, fluorescentChannelArrayList, brightfieldChannel, projectionMethod, degreeofFocusMethod);
    }
  }

  /* (non-Javadoc)
   * @see ij.gui.DialogListener#dialogItemChanged(ij.gui.GenericDialog, java.awt.AWTEvent)
   */
  @Override
  public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
  {
    @SuppressWarnings("unchecked")
    Vector<Choice> choices = gd.getChoices();

    //Re-populate the channel checkBoxes if brightfield channel has been change
    //If choice selection for brightfield channel has been modified
    if (e!=null && e.getSource()==choices.get(0))
    {
      String selectedChannel =  choices.get(0).getSelectedItem();
      @SuppressWarnings("unchecked")
      Vector<Checkbox> checkBoxVector = (Vector<Checkbox>) gd.getCheckboxes();
      for (int i=0; i<checkBoxVector.size();i++)
      {
        Checkbox cb = (Checkbox)checkBoxVector.get(i);
        if(cb.getLabel().equals(selectedChannel))
        {
          cb.setEnabled(false);
          cb.setState(false);
        }
        else
          cb.setEnabled(true);
      }
    }
    return true;
  }
  
  /**This method will display for each fluorescent channel, a image composite
   * where :<br/>
   * - channel 1 is the Z-Project of the fluorescent channel<br/>
   * - channel 2 is the best focus across Z plan of the brightfield channel.<br/>
   * Previous LUT will be preserved.
   * 
   * @param imp : the original hyperstack (XYZT)
   * @param fluorescentChannelList : the list of selected fluorescent channel number
   * @param brightfieldChannel : the brightfield channel number
   * @param zProjectionMethod : the Z-Projection {@link ij.plugin.ZProjector.METHODS}
   * used generate the channel 1. Options are: {@link ij.plugin.ZProjector.AVG_METHOD},
   * {@link ij.plugin.ZProjector.MAX_METHOD}, {@link ij.plugin.ZProjector.MIN_METHOD},
   * {@link ij.plugin.ZProjector.SUM_METHOD}, {@link ij.plugin.ZProjector.SD_METHOD} and
   * {@link ij.plugin.ZProjector.MEDIAN_METHOD}.
   */
  public void generateMIPBrightFieldStack(ImagePlus imp, ArrayList<Integer> fluorescentChannelList, int brightfieldChannel, int zProjectionMethod, int degreeofFocusMethod)
  {
    //1. Generate the BestFocus BrightField Stack
    ImagePlus brightfieldImp = computeAutoFocusStack(imp, brightfieldChannel, degreeofFocusMethod);
    //2. Keep the LUT
    LUT[] lutArray = imp.getLuts();    
    //3. For each selected fluorescent channel, compute the Maximum intensity 
    //   projection and create a new Stack with 1st channel is the MIP then 2nd
    //   channel is the best focus brightfield and display them
    if(fluorescentChannelList.size()>0)
      for (int i=0; i< fluorescentChannelList.size(); i++)
      {
        ImagePlus[] impArray = new ImagePlus[2];
        int mipChannel= fluorescentChannelList.get(i);
        System.out.println("MIP on channel :"+mipChannel);
        impArray[0]=getZProjection(imp, mipChannel, ZProjector.MAX_METHOD);
        if(i>0)
          impArray[1]=brightfieldImp.duplicate();
        else
          impArray[1]=brightfieldImp.duplicate();
        ImagePlus result = RGBStackMerge.mergeChannels(impArray,false);
        CompositeImage projImageComp = new CompositeImage(result);
        //Apply the original LUT
        projImageComp.setChannelLut(lutArray[mipChannel-1], 1);
        projImageComp.setChannelLut(lutArray[brightfieldChannel-1], 2);
        projImageComp.setTitle("Channel "+fluorescentChannelList.get(i));
        projImageComp.setMode(CompositeImage.COMPOSITE);
        projImageComp.show();
      }
    else
      brightfieldImp.show();
      
  }
  
  /**
   * @param imp : the original hyperstack (XYZT)
   * @param selectedChannel : the selected fluorescent channel number
   * @param method : the method used to compute ZProjection
   * @return the Z-Project of the fluorescent channel across time
   */
  public ImagePlus getZProjection(ImagePlus imp, int selectedChannel, int method)
  {
    ZProjector projector;
    int [] channelArray;
    ImagePlus projImage;
    
    projector = new ZProjector(imp);
    projector.setStopSlice(imp.getNSlices());
    projector.setMethod(method);
    channelArray = new int[1];
    channelArray[0]=selectedChannel;
    projector.doHyperStackProjection(true, channelArray);
    projImage = projector.getProjection();
    projImage.setCalibration(imp.getCalibration());

    return projImage;
  }  
  
  /**From a multiple channel Hyperstack, get the selected channel and return
   * the best autofocus Z slices accross time.
   * Works only with brightfield. Best focus is selected by being the one
   * across Z that have the highest mean intensity
   * @param imp : the original hyperstack (XYZT)
   * @param selectedChannel : the brightfield channel number
   * @return the best focus stack across time of the selected channel stack XYT
   */
  public ImagePlus computeAutoFocusStack(ImagePlus imp, int selectedChannel, int degreeofFocusMethod)
  {
    int zNumber = -1;
    int tNumber = -1;
    int width=-1;
    int height = -1;
    int bitdepth = -1;
    ImagePlus stack;
    ImageProcessor[] ipArray;
    ImageProcessor lastIP=null;
    
    zNumber = imp.getNSlices();
    tNumber = imp.getNFrames();
    width = imp.getWidth();
    height = imp.getHeight();
    bitdepth = imp.getBitDepth();
    if(bitdepth<=8)
      bitdepth=8;
    if(bitdepth>8 && bitdepth<=16)
      bitdepth=16;    
    stack = IJ.createHyperStack("AutoFocused",
        width,
        height,
        1,
        1,
        tNumber,
        bitdepth);    
    ipArray = new ImageProcessor[zNumber];
    for(int t=1;t<=tNumber;t++)
    {
      System.out.print("For time "+t+", ");
      ImageProcessor bestIp;
      IJ.showProgress(t, tNumber);
      for(int z=1;z<=zNumber;z++) 
      {
        ImageProcessor ip;
        int index = imp.getStackIndex(selectedChannel, z, t);
        imp.setSliceWithoutUpdate(index);
        ip = imp.getProcessor().duplicate();
        ipArray[z-1]=ip;
      }
      bestIp = getBestZFocus(ipArray, degreeofFocusMethod); 
      stack.setPositionWithoutUpdate(1, 1, t);
      stack.setProcessor(bestIp);
      if(t==tNumber-1)
        lastIP=bestIp;
    }
    //Weird hack since the last slice is not properly added
    //Remove the last slice that is black (empty)
    stack.getStack().deleteLastSlice();
    //Replace it with the last slice (which was supposed to be set at line stack.setProcessor(bestIp);)
    stack.getStack().addSlice(lastIP);
    stack.setPositionWithoutUpdate(1, 1, 1);
    
    return stack;
  }

  //TODO: A much better algorithm should be used it:
  //See for e.g. the following paper http://onlinelibrary.wiley.com/doi/10.1111/j.1365-2818.2010.03389.x/full
  //and see http://www.mathworks.com/matlabcentral/fileexchange/27314-focus-measure/content/fmeasure/fmeasure.m for more possible implementation
  /** Get the best Z-focus plan by choosing between an array of imageProcessor using multiple methods
   * @param ipArray the array of image processor
   * @return the supposedly best focus Z-plan AKA the one with the highest mean intensity
   */
  private ImageProcessor getBestZFocus(ImageProcessor[] ipArray, int method)
  {
    int best=0;
    double quality=0;
    double measure=0;
    for(int z = 0; z<ipArray.length; z++)
    {
      ImageProcessor ipstk = ipArray[z];
      measure = FocusMeasure.getDegreeofFocus(ipstk, method);
      if(quality<measure)
      {
        quality=measure;
        best=z;
      }
    }
    return ipArray[best].duplicate();
  }

}
