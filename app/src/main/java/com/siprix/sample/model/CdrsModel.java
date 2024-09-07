package com.siprix.sample.model;

import java.util.ArrayList;
import java.util.List;

//CallDetailsRecord

public class CdrsModel {
    final private List<CdrModel> cdrItems_ = new ArrayList<CdrModel>();
    final public int kMaxItems=10;

    protected CdrsModel() {

    }
    private CdrModel find(int callId) {
        for (int i = 0; i < cdrItems_.size(); i++) {
            if(cdrItems_.get(i).getCallId() == callId)
                return cdrItems_.get(i);
        }
        return null;
    }

    void add(CallModel c) {
        _add(new CdrModel(c));
    }

    void _add(CdrModel cdr) {
        cdrItems_.add(cdr);
        if(cdrItems_.size() > kMaxItems) {
            cdrItems_.remove(0);
        }

        notifyListeners();
    }

    void setConnected(int callId, String from, String to, boolean withVideo) {
        CdrModel cdr = find(callId);
        if(cdr==null) return;

        cdr.setConnected(from, to, withVideo);
        notifyListeners();
    }

    void setTerminated(int callId, int statusCode, String durationStr) {
        CdrModel cdr = find(callId);
        if(cdr==null) return;

        cdr.setTerminated(statusCode, durationStr);

        notifyListeners();
        notifySaveChanges();
    }

    void remove(int index) {
        if((index>=0)&&(index < cdrItems_.size())) {
            cdrItems_.remove(index);
            notifyListeners();
        }
    }

    boolean loadFromJson(String cdrsJsonStr) {
        return false;//TODO add impl
        //try {
        //    if(cdrsJsonStr.isEmpty()) return false;
        //    final parsedList = (jsonDecode(cdrsJsonStr) as List).cast<Map<String, dynamic>>();
        //    for (var parsedCdr in parsedList.reversed) {
        //        _add(CdrModel.fromJson(parsedCdr));
        //    }
        //    return parsedList.isNotEmpty;
        //}catch (e) {
        //    return false;
        //}
    }

    String storeToJson() {
        //return jsonEncode(cdrItems_);
        return "";//TODO add impl
    }

    public void notifyListeners() {
        //TODO add impl
    }

    public void notifySaveChanges() {
        //TODO add impl
    }
}
