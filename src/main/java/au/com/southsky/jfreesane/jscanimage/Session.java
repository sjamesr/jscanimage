package au.com.southsky.jfreesane.jscanimage;

import au.com.southsky.jfreesane.SaneDevice;
import au.com.southsky.jfreesane.SaneSession;
import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Session {
  private boolean warnedUser = false;
  private boolean shouldQuit = false;
  private String hostName;
  private SaneSession session;
  private SaneDevice currentDevice;
  private List<BufferedImage> images = new ArrayList<>();
  private List<Instant> acquisitionTimes = new ArrayList<>();
  private JCommander mainJCommander;
  private final List<SaneDevice> devicesSeen = new ArrayList<>();

  public boolean shouldQuit() {
    return shouldQuit;
  }

  public void close() throws IOException {
    session.close();
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public void setSaneSession(SaneSession session) {
    this.session = session;
  }

  public String getHostName() {
    return hostName;
  }

  public SaneDevice getCurrentDevice() {
    return currentDevice;
  }

  public void setWarnedUser(boolean warnedUser) {
    this.warnedUser = warnedUser;
  }

  public List<BufferedImage> getImages() {
    return ImmutableList.copyOf(images);
  }

  public boolean didWarnUser() {
    return warnedUser;
  }

  public void setCurrentDevice(SaneDevice currentDevice) {
    this.currentDevice = currentDevice;
  }

  public void clearImages() {
    images.clear();
    acquisitionTimes.clear();
  }

  public void addImage(BufferedImage image) {
    images.add(image);
    acquisitionTimes.add(Instant.now());
  }

  public void setShouldQuit(boolean shouldQuit) {
    this.shouldQuit = shouldQuit;
  }

  public List<Instant> getAcquisitionTimes() {
    return ImmutableList.copyOf(acquisitionTimes);
  }

  public SaneSession getSaneSession() {
    return session;
  }

  public JCommander getMainJCommander() {
    return mainJCommander;
  }

  public void setMainJCommander(JCommander mainJCommander) {
    this.mainJCommander = mainJCommander;
  }

  public void addSeenDevice(SaneDevice device) {
    devicesSeen.add(device);
  }

  public List<SaneDevice> getDevicesSeen() {
    return devicesSeen;
  }

  public void clearDevicesSeen() {
    devicesSeen.clear();
  }
}
