package com.siprix.sample.model;

import com.siprix.SiprixCore;

import java.util.ArrayList;

public class DevicesModel {
    private final ObjModel parent_;
    private final ArrayList<SiprixCore.AudioDevice> dvcItems_;
    private SiprixCore.AudioDevice selItem_;
    private ModelObserver observer_;

    protected DevicesModel(ObjModel parent) {
        dvcItems_ = new ArrayList<SiprixCore.AudioDevice>();
        parent_ = parent;
    }

    public SiprixCore.AudioDevice get(int index) {   return dvcItems_.get(index);  }
    public boolean isEmpty() { return dvcItems_.isEmpty(); }
    public int size() { return dvcItems_.size(); }

    public void switchCamera() {
        parent_.core_.dvcSwitchCamera();
    }

    public boolean isSelected(SiprixCore.AudioDevice dvc) {
        return dvc==selItem_;
    }

    public void selectDevice(int index) {
        if((index>=0)&&(index < dvcItems_.size())) {
            selectDevice(dvcItems_.get(index));
        }
    }

    public void selectDevice(SiprixCore.AudioDevice dvc) {
        parent_.log("Select audio device: "+dvc.name());
        parent_.core_.dvcSetAudioDevice(dvc);
        selItem_ = dvc;
        notifyListeners();
    }

    void onDevicesAudioChanged() {
        dvcItems_.clear();
        int number = parent_.core_.dvcGetAudioDevices();
        for(int i=0; i<number; i++) {
            dvcItems_.add(parent_.core_.dvcGetAudioDevice(i));
        }
        selItem_ = parent_.core_.dvcGetSelAudioDevice();

        notifyListeners();
    }

    public void setObserver(ModelObserver observer) {
        observer_ = observer;
    }

    public void notifyListeners() {
        if(observer_!=null)
            observer_.onModelChanged();
    }
}
