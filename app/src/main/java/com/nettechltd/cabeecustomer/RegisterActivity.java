package com.nettechltd.cabeecustomer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.andreabaccega.widget.FormEditText;
import com.devmarvel.creditcardentry.library.CreditCard;
import com.devmarvel.creditcardentry.library.CreditCardForm;
import com.eftimoff.androidplayer.Player;
import com.nettechltd.cabeecustomer.Core.Animations;
import com.nettechltd.cabeecustomer.Core.Webservices;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.exception.AuthenticationException;

import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Asim on 2/6/2015 while working for NetTech Private Limited.
 */
public class RegisterActivity extends AppCompatActivity
{
    Context con = RegisterActivity.this;

    @InjectView(R.id.name) FormEditText fullName_et;
    @InjectView(R.id.email) FormEditText email_et;
    @InjectView(R.id.mobile) FormEditText mobile_et;
    @InjectView(R.id.password) FormEditText password_et;
    @InjectView(R.id.confirmpassword) FormEditText confirmPass_et;
    @InjectView(R.id.home_address) FormEditText homeAddress_et;
    @InjectView(R.id.payment_method_rb_group) RadioGroup paymentMethodGroup;
    @InjectView(R.id.cash_rb) RadioButton paymentCash_rb;
    @InjectView(R.id.cc_rb) RadioButton paymentCC_rb;
    @InjectView(R.id.account_rb) RadioButton paymentAccount_rb;
    @InjectView(R.id.account_name) FormEditText accountName_et;
    @InjectView(R.id.account_number) FormEditText accountNumber_et;
    @InjectView(R.id.register) Button register_bt;
    @InjectView(R.id.cancel) Button cancel_bt;
    @InjectView(R.id.credit_card_form) CreditCardForm ccForm;
    @InjectView(R.id.layout_acc) LinearLayout accountLayout;
    @InjectView(R.id.layout_cc) LinearLayout creditCardLayout;

    String paymentMethod = "C";

    MaterialDialog d;

    @Override
    protected void onCreate(Bundle savedBundleInstance)
    {
        super.onCreate(savedBundleInstance);
        setContentView(R.layout.activity_register);
        ButterKnife.inject(this);

        paymentMethodGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                switch (checkedId)
                {
                    case R.id.cash_rb:
                        paymentCash_rb.toggle();
                        paymentMethod = "Cash";
                        accountLayout.setVisibility(View.GONE);
                        creditCardLayout.setVisibility(View.GONE);
                        break;

                    case R.id.cc_rb:
                        paymentCC_rb.toggle();
                        paymentMethod = "CreditCard";
                        accountLayout.setVisibility(View.GONE);
                        creditCardLayout.setVisibility(View.VISIBLE);
                        Player.init().animate(new Animations().slideDown(creditCardLayout, 300)).play();
                        break;

                    case R.id.account_rb:
                        paymentAccount_rb.toggle();
                        paymentMethod = "Account";
                        accountLayout.setVisibility(View.VISIBLE);
                        creditCardLayout.setVisibility(View.GONE);
                        Player.init().animate(new Animations().slideDown(accountLayout, 300)).play();
                        break;

                    default:
                        paymentCash_rb.toggle();
                        paymentMethod = "C";
                        break;
                }
            }
        });

        register_bt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                FormEditText[] validationFieldsCash = {fullName_et, email_et, mobile_et, password_et, confirmPass_et, homeAddress_et};

                if (Validate(validationFieldsCash))
                {
                    if (paymentMethod.equalsIgnoreCase("CC"))
                    {
                        if (ccForm.isCreditCardValid())
                        {
                            CreditCard cc = ccForm.getCreditCard();
                            Card creditCard = new Card(cc.getCardNumber(), cc.getExpMonth(), cc.getExpYear(), cc.getSecurityCode());
                            try
                            {
                                Stripe s = new Stripe("pk_test_bEIUUtjKUpotifKburb5jNx2");
                                s.createToken(creditCard, new TokenCallback()
                                {
                                    @Override
                                    public void onError(Exception e)
                                    {
                                        e.printStackTrace();
                                    }

                                    @Override
                                    public void onSuccess(Token token)
                                    {
                                        new Register().execute(fullName_et.getText().toString(),
                                                email_et.getText().toString(),
                                                password_et.getText().toString(),
                                                mobile_et.getText().toString(),
                                                homeAddress_et.getText().toString(),
                                                paymentMethod);
                                    }
                                });

                            } catch (AuthenticationException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if(paymentMethod.equalsIgnoreCase("A"))
                    {

                    }
                    else
                    {
                        new Register().execute(fullName_et.getText().toString(),
                                email_et.getText().toString(),
                                password_et.getText().toString(),
                                mobile_et.getText().toString(),
                                homeAddress_et.getText().toString(),
                                paymentMethod);
                    }
                }
                else
                {
                    // Lib handles displaying of validation errors set in the xml
                }
            }
        });

        cancel_bt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(con, LoginActivity.class));
                finish();
            }
        });
    }

    public boolean Validate(FormEditText[] fields)
    {
        boolean allValid = true;

        for (FormEditText field : fields)
        {
            allValid = field.testValidity() && allValid;
        }

        return allValid;
    }

    private class Register extends AsyncTask<String, Void, String>
    {
        @Override
        protected void onPreExecute()
        {
            d = new MaterialDialog.Builder(con)
                    .customView(R.layout.dialog_progress, false)
                    .build();
            d.show();
        }

        @Override
        protected String doInBackground(String... params)
        {
            return new Webservices(con).Register(params[0], params[1], params[2], params[3], params[4], params[5]);
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (Pattern.compile("Success", Pattern.CASE_INSENSITIVE).matcher(result).find())
            {
                startActivity(new Intent(con, LoginActivity.class));
                finish();
            }
            else if (Pattern.compile("Already", Pattern.CASE_INSENSITIVE).matcher(result).find())
            {
                final Snackbar snack = Snackbar.make(findViewById(R.id.root_layout), result, Snackbar.LENGTH_LONG);
                snack.setAction("Retry", new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        snack.dismiss();
                    }
                });
                snack.show();
            }
            else
            {
                final Snackbar snack = Snackbar.make(findViewById(R.id.root_layout), result, Snackbar.LENGTH_LONG);
                snack.setAction("Dismiss", new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        snack.dismiss();
                    }
                });
                snack.show();
            }

            if (d.isShowing())
            {
                d.dismiss();
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        startActivity(new Intent(con, LoginActivity.class));
        finish();
    }
}
