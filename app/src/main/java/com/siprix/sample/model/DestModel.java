package com.siprix.sample.model;

import com.siprix.AccData;
import com.siprix.DestData;

import java.util.HashMap;
import java.util.Map;

public class DestModel {
    private String toExt_ = "";
    private int    fromAccId_ = 0;
    private boolean  withVideo_ = false;
    private HashMap<String, String> xheaders;

    public String getToExt() { return toExt_; }
    public void setToExt(String s) { toExt_ = s; }

    public int getSrcAccId() { return fromAccId_; }
    public void setSrcAccId(int id) { fromAccId_ = id; }

    public boolean getWithVideo() { return withVideo_; }
    public void setWithVideo(boolean v) { withVideo_ = v; }

    public void addXHeader(String hdr, String val) {
        if(xheaders == null)
            xheaders = new HashMap<String, String>();

        xheaders.put(hdr, val);
    }

    DestData getData() {
        DestData dst = new DestData();
        dst.setExtension(toExt_);
        dst.setAccountId(fromAccId_);
        dst.setVideoCall(withVideo_);

        if(xheaders != null) {
            for (Map.Entry<String, String> entry : xheaders.entrySet()) {
                dst.addXHeader(entry.getKey(), entry.getValue());
            }
        }
        return dst;
    }
}
