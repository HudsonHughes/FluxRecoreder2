package com.hudson.fluxrecorder;

import android.app.Activity;

import org.solovyev.android.checkout.EmptyRequestListener;
import org.solovyev.android.checkout.Purchase;

import javax.annotation.Nonnull;

/**
 * Created by VGDCUser on 3/1/2018.
 */
class PurchaseListener extends EmptyRequestListener<Purchase> {
    Activity activity;
    public PurchaseListener(Activity activity){
        super();
        this.activity = activity;
    }

    @Override
    public void onError(int response, @Nonnull Exception e) {
        super.onError(response, e);
    }

    @Override
    public void onSuccess(@Nonnull Purchase purchase) {

    }
}