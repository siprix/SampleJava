package com.siprix.sample.model;

import android.text.TextUtils;
import android.util.Log;

import com.siprix.AccData;
import com.siprix.SiprixCore;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;


public class AccountsModel {
    private static final String TAG = "AccountsModel";
    final private List<AccountModel> accItems_ = new ArrayList<>();
    final private ObjModel parent_;
    private ModelObserver observer_;
    private int selAccId_ = 0;

    protected AccountsModel(ObjModel parent) {
        parent_ = parent;
    }

    public AccountModel get(int index) {  return accItems_.get(index);  }
    public boolean isEmpty() { return accItems_.isEmpty(); }
    public int size() { return accItems_.size(); }

    public int getSelectedAccPosition() { return findAccountIdx(selAccId_); }
    public int getSelectedAccId() { return selAccId_; }
    public AccountModel getSelectedAcc() {
        int idx = findAccountIdx(selAccId_);
        return (idx==-1) ? null : accItems_.get(idx);
    }

    private int findAccountIdx(int accId) {
        for (int i = 0; i < accItems_.size(); i++) {
            if(accItems_.get(i).getAccId() == accId)
                return i;
        }
        return -1;
    }

    String getUri(int accId) {
        int idx = findAccountIdx(accId);
        return (idx==-1) ? "?" : accItems_.get(idx).getUri();
    }
    boolean hasSecureMedia(int accId) {
        int idx = findAccountIdx(accId);
        return (idx != -1) && accItems_.get(idx).hasSecureMedia();
    }

    public boolean isNetworkLost() { return parent_.netState_.isNetworkLost(); }

    public void setObserver(ModelObserver observer) {
        observer_ = observer;
    }

    public boolean add(AccountModel accModel) throws Exception {
        return addAccountImpl(accModel, true);
    }

    boolean addAccountImpl(AccountModel accModel, boolean saveChanges) throws Exception {
        parent_.log("Adding new account: "+ accModel.getUri());

        AccData accData = accModel.getData();//Prepare 'Siprix.AccData'
        appendPushTokenToAccount(accData);//Add pushtoken to data

        SiprixCore.IdOutArg accIdArg = new SiprixCore.IdOutArg();
        int err = parent_.core_.accountAdd(accData, accIdArg);
        if(err != SiprixCore.kOK)
            throw new Exception(parent_.getErrText(err));
        
        accModel.setAccId(accIdArg.value);
        if(accModel.getExpireTime()==0) {
            accModel.setRegText("Registration removed");
            accModel.setRegState(AccData.RegState.REMOVED);
        }

        accItems_.add(accModel);

        if(selAccId_ == 0) {
            selAccId_ = accModel.getAccId();
        }

        notifyListeners();

        if(saveChanges)
            postSaveChanges();

        parent_.log("Added successfully with id: "+ accModel.getAccId());
        return true;
    }

    public boolean remove(int accId) throws Exception {
        int index = findAccountIdx(accId);
        if(index ==-1) return false;

        int err = parent_.core_.accountDelete(accId);
        if(err != SiprixCore.kOK)
            throw new Exception(parent_.getErrText(err));

        if(selAccId_ == accId) {
            selAccId_ = accItems_.isEmpty() ? 0 : accItems_.get(0).getAccId();
        }

        accItems_.remove(index);
        notifyListeners();
        postSaveChanges();
        parent_.log("Deleted account accId:"+accId);
        return true;
    }

    public boolean unregister(int accId) throws Exception {
        int index = findAccountIdx(accId);
        if(index ==-1) return false;

        int err = parent_.core_.accountUnregister(accId);
        if(err != SiprixCore.kOK)
            throw new Exception(parent_.getErrText(err));

        AccountModel accModel = accItems_.get(index);
        accModel.regState_ = AccData.RegState.INPROGRES;
        accModel.regText_ = "";
        accModel.setExpireTime(0);
        notifyListeners();
        postSaveChanges();
        parent_.log("Unregistering accId:"+accId);
        return true;
    }

    public boolean register(int accId, int expireTimeSec) throws Exception{
        int index = findAccountIdx(accId);
        if(index ==-1) return false;

        int err = parent_.core_.accountRegister(accId, expireTimeSec);
        if(err != SiprixCore.kOK)
            throw new Exception(parent_.getErrText(err));

        AccountModel accModel = accItems_.get(index);
        accModel.regState_ = AccData.RegState.INPROGRES;
        accModel.regText_ = "";
        accModel.setExpireTime(expireTimeSec);
        notifyListeners();
        postSaveChanges();
        parent_.log("Refreshing registration accId:"+accId);
        return true;
    }

    public void setSelectedAccId(int accId) {
        selAccId_ = accId;
        notifyListeners();
        parent_.log("setSelectedAccId accId:"+accId);
    }

    void onRegStateChanged(int accId, AccData.RegState state, String response) {
        int index = findAccountIdx(accId);
        if(index ==-1) return;

        AccountModel accModel = accItems_.get(index);
        accModel.setRegText(response);
        accModel.setRegState(state);
        notifyListeners();
    }

    void onNetworkState(String name, SiprixCore.NetworkState state) {
        notifyListeners();
    }

    void appendPushTokenToAccount(AccData accData) {
        if(TextUtils.isEmpty(parent_.getFcmPushToken())||
           TextUtils.isEmpty(parent_.getFcmProjectId())) {
            return;
        }
        //Add push token in the way supported by your app.
        //Adding as separate header sent in REGISTER request
        //accData.addXHeader("X-PushToken", parent_.getFmcPushToken());

        //Adding as ContactUriParams sent in Contact header of REGISTER request (RFC8599 format)
        accData.addXContactUriParam("pn-prid", parent_.getFcmPushToken());
        accData.addXContactUriParam("pn-param", parent_.getFcmProjectId());
        accData.addXContactUriParam("pn-provider", "fcm");
    }

    void refreshRegistration() {
        for (AccountModel acc : accItems_) {
            if(acc.getExpireTime()==0) continue;

            int err = parent_.core_.accountRegister(acc.getAccId(), acc.getExpireTime());
            if(err != SiprixCore.kOK)
                Log.e(getClass().getName(), parent_.getErrText(err));
            else
                acc.regState_ = AccData.RegState.INPROGRES;
        }
    }


    void loadFromJson(String accJsonStr) {
        if(TextUtils.isEmpty(accJsonStr)|| !accItems_.isEmpty()) {
            //Skip when input string empty or accounts already present
            return;
        }

        //Restore saved
        try {
            JSONArray jsonRootArr = new JSONArray(accJsonStr);

            for(int i=0; i<jsonRootArr.length(); i++) {
                AccountModel acc = new AccountModel();
                acc.loadFromJson(jsonRootArr.getJSONObject(i));
                addAccountImpl(acc, false);
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
        }
    }

    String storeToJson() {
        try {
            JSONArray jsonRootArr = new JSONArray();
            for (AccountModel acc : accItems_) {
                jsonRootArr.put(acc.storeToJson());
            }
            return jsonRootArr.toString();
        }
        catch (JSONException e) {
            Log.e(getClass().getName(), e.toString());
            return "";
        }
    }
    
    private void notifyListeners() {
        if(observer_!=null) 
            observer_.onModelChanged();
    }

    private void postSaveChanges() {
        parent_.postSaveAccounts();
    }
}
