package com.siprix.sample.model;

import com.siprix.SiprixCore;

public class NetworkStateModel {
    final private ObjModel parent_;
    private ModelObserver observer_;
    private String networkName_ = "";
    private boolean networkLost_ = false;

    protected NetworkStateModel(ObjModel parent) {
        parent_ = parent;
    }

    public String getNetworkName() { return networkName_; }
    public boolean isNetworkLost() { return networkLost_; }

    public void onNetworkState(String name, SiprixCore.NetworkState state) {
        networkName_ = name;
        networkLost_ = (state==SiprixCore.NetworkState.LOST);
        notifyListeners();
    }

    private void notifyListeners() {
        if(observer_!=null)
            observer_.onModelChanged();
    }

    public void setObserver(ModelObserver observer) {
        observer_ = observer;
    }
}
