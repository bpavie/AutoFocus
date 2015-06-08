package be.vib.autofocus;

import ij.process.ImageProcessor;

public class BestFocusResult {

  ImageProcessor ip;
  int position;
  
  public ImageProcessor getIp() {
    return ip;
  }
  public void setIp(ImageProcessor ip) {
    this.ip = ip;
  }
  public int getPosition() {
    return position;
  }
  public void setPosition(int position) {
    this.position = position;
  }
}
