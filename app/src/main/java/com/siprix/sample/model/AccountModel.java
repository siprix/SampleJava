package com.siprix.sample.model;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.siprix.AccData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountModel {
    int myAccId_ = 0;
    AccData.RegState regState_ = AccData.RegState.INPROGRES;
    String regText_="";

    private String sipServer_ = "";
    private String sipExtension_ = "";
    private String sipPassword_ = "";

    private String sipAuthId_;
    private String sipProxy_;
    private String displName_;
    private String userAgent_;

    private int    expireTime_ = kDefExpireTimeSec;
    private AccData.SipTransport transport_;
    private AccData.SecureMediaMode secureMedia_;
    private Integer port_;
    private Boolean rewriteContactIp_;

    private HashMap<String, String> xheaders;
    private List<AccData.AudioCodec> audioCodecs;
    private List<AccData.VideoCodec> videoCodecs;

    public AccountModel() {
    }

    public int getAccId() { return myAccId_; }
    void setAccId(int id) { myAccId_ = id; }

    public AccData.RegState getState() { return regState_; }

    AccData.RegState getRegState() { return regState_; }
    void setRegState(AccData.RegState s) { regState_ = s; }

    public String getRegText() { return regText_; }
    void setRegText(String s) { regText_ = s; }

    @NonNull
    public String toString()   { return getUri();   }
    public String getUri()     { return sipExtension_ + "@"+ sipServer_;    }
    public int getExpireTime() { return expireTime_;   }

    public void setSipServer(String s)      {  sipServer_ = s;    }
    public void setSipExtension(String s)   {  sipExtension_ = s; }
    public void setSipPassword(String s)    {  sipPassword_ = s;  }

    public void setSipAuthId(String s)      {  sipAuthId_ = s;    }
    public void setSipProxyServer(String s) {  sipProxy_  = s;    }
    public void setRewriteContactIp(Boolean b) { rewriteContactIp_ = b;}

    public boolean hasSecureMedia() {
        return (secureMedia_!=null)&&(secureMedia_!= AccData.SecureMediaMode.DISALED); }
    public AccData.SecureMediaMode getSecureMedia() {
        return (secureMedia_!=null) ? secureMedia_  : AccData.SecureMediaMode.DISALED; }
    public void setSecureMedia(AccData.SecureMediaMode mode) { secureMedia_ = mode; }

    public void setSipTranspProtocol(AccData.SipTransport s) {  transport_ = s; }
    public void setExpireTime(int t)  {  expireTime_ = t;   }
    public void setPort(int port)     {  port_ = port;      }

    public static final int kDefExpireTimeSec = 300;

    public void addXHeader(String hdr, String val) {
        if(xheaders == null)
           xheaders = new HashMap<String, String>();

        xheaders.put(hdr, val);
    }

    public void resetAudioCodecs() {
        audioCodecs = new ArrayList<>();
    }

    public void resetVideoCodecs() {
        videoCodecs = new ArrayList<>();
    }

    public void addAudioCodec(AccData.AudioCodec c) {
        audioCodecs.add(c);
    }

    public void addVideoCodec(AccData.VideoCodec c) {
        videoCodecs.add(c);
    }


    AccData getData() {
        AccData acc = new AccData();
        acc.setSipServer(sipServer_);
        acc.setSipExtension(sipExtension_);
        acc.setSipPassword(sipPassword_);
        acc.setExpireTime(expireTime_);

        if(!TextUtils.isEmpty(sipAuthId_)) acc.setSipAuthId(sipAuthId_);
        if(!TextUtils.isEmpty(sipProxy_))  acc.setSipProxyServer(sipProxy_);
        if(!TextUtils.isEmpty(displName_)) acc.setDisplayName(displName_);
        if(!TextUtils.isEmpty(userAgent_)) acc.setUserAgent(userAgent_);

        if(rewriteContactIp_!=null) acc.setRewriteContactIp(rewriteContactIp_);
        if(secureMedia_ != null) acc.setSecureMediaMode(secureMedia_);
        if(transport_  != null)  acc.setTranspProtocol(transport_);
        if(port_ != null)        acc.setTranspPort(port_);

        if(xheaders != null) {
            for (Map.Entry<String, String> entry : xheaders.entrySet()) {
                acc.addXHeader(entry.getKey(), entry.getValue());
            }
        }

        if((audioCodecs!=null)&&(!audioCodecs.isEmpty())) {
            acc.resetAudioCodecs();
            for(AccData.AudioCodec c : audioCodecs)
                acc.addAudioCodec(c);
        }

        if((videoCodecs!=null)&&(!videoCodecs.isEmpty())) {
            acc.resetVideoCodecs();
            for(AccData.VideoCodec c : videoCodecs)
                acc.addVideoCodec(c);
        }

        return acc;
    }

    JSONObject storeToJson() throws JSONException {
        JSONObject jsonAcc = new JSONObject();

        jsonAcc.put("SipServer",    sipServer_);
        jsonAcc.put("SipExtension", sipExtension_);
        jsonAcc.put("SipPassword",  sipPassword_);
        jsonAcc.put("ExpireTime",   expireTime_);

        if(!TextUtils.isEmpty(sipAuthId_)) jsonAcc.put("SipAuthId", sipAuthId_);
        if(!TextUtils.isEmpty(sipProxy_))  jsonAcc.put("SipProxyServer", sipProxy_);
        if(!TextUtils.isEmpty(displName_)) jsonAcc.put("DisplayName", displName_);
        if(!TextUtils.isEmpty(userAgent_)) jsonAcc.put("UserAgent", userAgent_);

        if(rewriteContactIp_!= null) jsonAcc.put("RewriteContactIp", rewriteContactIp_);
        if(secureMedia_ != null) jsonAcc.put("SecureMedia", secureMedia_.getValue());
        if(transport_  != null)  jsonAcc.put("TranspProtocol", transport_.getValue());
        if(port_ != null)        jsonAcc.put("TranspPort", port_);

        if(xheaders != null) {
            JSONArray headersArr = new JSONArray();
            for (Map.Entry<String, String> entry : xheaders.entrySet()) {
                JSONObject hdrVal = new JSONObject();
                hdrVal.put("hdr", entry.getKey());
                hdrVal.put("val", entry.getValue());
                headersArr.put(hdrVal);
            }
            jsonAcc.put("xheaders", headersArr);
        }
        return jsonAcc;
    }//storeToJson

    void loadFromJson(JSONObject jsonAcc) throws JSONException {
        sipServer_   = jsonAcc.getString("SipServer");
        sipExtension_= jsonAcc.getString("SipExtension");
        sipPassword_ = jsonAcc.getString("SipPassword");

        if(jsonAcc.has("SipAuthId"))
            sipAuthId_ = jsonAcc.getString("SipAuthId");
        if(jsonAcc.has("SipProxyServer"))
            sipProxy_  = jsonAcc.getString("SipProxyServer");
        if(jsonAcc.has("DisplayName"))
            displName_ = jsonAcc.getString("DisplayName");
        if(jsonAcc.has("UserAgent"))
            userAgent_ = jsonAcc.getString("UserAgent");

        if(jsonAcc.has("SecureMedia"))
            secureMedia_ = AccData.SecureMediaMode.fromInt(jsonAcc.getInt("SecureMedia"));

        if(jsonAcc.has("TranspProtocol"))
            transport_ = AccData.SipTransport.fromInt(jsonAcc.getInt("TranspProtocol"));

        if(jsonAcc.has("ExpireTime"))
            expireTime_ = jsonAcc.getInt("ExpireTime");
        if(jsonAcc.has("TranspPort"))
            port_ = jsonAcc.getInt("TranspPort");
        if(jsonAcc.has("RewriteContactIp"))
            rewriteContactIp_ = jsonAcc.getBoolean("RewriteContactIp");

        if(xheaders != null) {
            JSONArray headersArr = jsonAcc.getJSONArray("xheaders");
            for(int i=0; i<headersArr.length(); i++) {
                JSONObject hdrVal = headersArr.getJSONObject(i);
                xheaders.put(hdrVal.getString("hdr"), hdrVal.getString("val"));
            }
        }
    }//loadFromJson
}
