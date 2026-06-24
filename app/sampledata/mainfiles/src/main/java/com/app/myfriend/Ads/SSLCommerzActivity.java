package com.app.myfriend.Ads;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.app.myfriend.R;
import com.sslcommerz.library.payment.model.datafield.MandatoryFieldModel;
import com.sslcommerz.library.payment.model.dataset.TransactionInfo;
import com.sslcommerz.library.payment.model.util.CurrencyType;
import com.sslcommerz.library.payment.model.util.ErrorKeys;
import com.sslcommerz.library.payment.model.util.SdkCategory;
import com.sslcommerz.library.payment.model.util.SdkType;
import com.sslcommerz.library.payment.viewmodel.listener.OnPaymentResultListener;
import com.sslcommerz.library.payment.viewmodel.management.PayUsingSSLCommerz;

public class SSLCommerzActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sslcommerz);

        MandatoryFieldModel mandatoryFieldModel = new MandatoryFieldModel("testbox", "qwerty", "10", "1232132321", CurrencyType.BDT, SdkType.TESTBOX, SdkCategory.BANK_LIST);

        PayUsingSSLCommerz.getInstance().setData(SSLCommerzActivity.this, mandatoryFieldModel, new OnPaymentResultListener() {
            @Override
            public void transactionSuccess(TransactionInfo transactionInfo) {
                // If payment is success and risk label is 0 get payment details from here
                if (transactionInfo.getRiskLevel().equals("0")) {


                       /* After successful transaction send this val id to your server and from
                         your server you can call this api
                         https://sandbox.sslcommerz.com/validator/api/validationserverAPI.php?val_id=yourvalid&store_id=yourstoreid&store_passwd=yourpassword
                         if you call this api from your server side you will get all the details of the transaction.
                         for more details visit:   www.tashfik.me
            */
                }
// Payment is success but payment is not complete yet. Card on hold now.
                else {

                }
            }

            @Override
            public void transactionFail(String s) {

            }


            @Override
            public void error(int errorCode) {
                switch (errorCode) {
// Your provides information is not valid.
                    case ErrorKeys.USER_INPUT_ERROR:

                        break;
// Internet is not connected.
                    case ErrorKeys.INTERNET_CONNECTION_ERROR:

                        break;
// Server is not giving valid data.
                    case ErrorKeys.DATA_PARSING_ERROR:

                        break;
// User press back button or canceled the transaction.
                    case ErrorKeys.CANCEL_TRANSACTION_ERROR:

                        break;
// Server is not responding.
                    case ErrorKeys.SERVER_ERROR:

                        break;
// For some reason network is not responding
                    case ErrorKeys.NETWORK_ERROR:

                        break;
                }
            }
        });

    }
}