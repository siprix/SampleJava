package com.siprix.sample.model;

public class CdrModel {
    private int myCallId_ = 0;
    private String remoteExt_ = "";
    private String accUri_ = "";
    private String duration_ = "";
    private boolean withVideo_ = false;
    private final boolean isIncoming_;
    private boolean isConnected_ = false;
    private int statusCode_ = 0;

    protected CdrModel(CallModel c) {
        this.myCallId_ = c.getCallId();
        this.remoteExt_ = c.getRemoteExt();
        this.accUri_ = c.getAccUri();
        this.isIncoming_ = c.getIsIncoming();
        this.withVideo_ = c.hasVideo();
    }

    String getRemoteExt() {
        return remoteExt_;
    }
    String getAccUri() {
        return accUri_;
    }
    int getCallId() {
        return myCallId_;
    }

    public void setConnected(String from, String to, boolean withVideo) {
        withVideo_   = withVideo;
        isConnected_ = true;
    }

    public void setTerminated(int statusCode, String durationStr) {
        statusCode_ = statusCode;
        duration_ = isConnected_ ? durationStr : "";
    }
}
