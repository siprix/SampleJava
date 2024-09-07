package com.siprix.sample.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.siprix.sample.MainActivity;
import com.siprix.sample.model.ObjModel;

public class BaseFragment extends Fragment {
    //React on 'ModelCreated'
    public void onModelCreated(ObjModel objModel) {
        setModel(objModel);
    }

    //Get model from activity and set to fragment
    protected void setModelToFragment() {
        MainActivity ma = (MainActivity)getActivity();
        if((ma != null)&&(ma.getObjModel() != null))
            setModel(ma.getObjModel());
    }

    protected void setModel(ObjModel objModel) {
        //Override in child class
    }
}
