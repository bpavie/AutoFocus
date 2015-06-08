package be.vib.autofocus;

import ij.ImagePlus;

public class BestFocusStackResult {

  ImagePlus stack;
  int[] positionArray;
  
  public ImagePlus getStack() {
    return stack;
  }
  public void setStack(ImagePlus stack) {
    this.stack = stack;
  }
  public int[] getPositionArray() {
    return positionArray;
  }
  public void setPositionArray(int[] positionArray) {
    this.positionArray = positionArray;
  }
  
  
}
