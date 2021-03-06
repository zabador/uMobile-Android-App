package org.apereo.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;
import org.apereo.App;
import org.apereo.R;
import org.apereo.constants.AppConstants;
import org.apereo.deserializers.LayoutDeserializer;
import org.apereo.models.Layout;
import org.apereo.services.RestApi;
import org.apereo.services.UmobileRestCallback;
import org.apereo.utils.LayoutManager;
import org.apereo.utils.Logger;

/**
 * Created by schneis on 8/28/14.
 */
@EActivity(R.layout.login_page)
public class LoginActivity extends BaseActivity {

    private static final String TAG = LoginActivity.class.getName();

    private final String ACCOUNT_TYPE = App.getInstance().getResources().getString(R.string.account_type);

    @ViewById(R.id.login_container)
    RelativeLayout container;

    @ViewById(R.id.web_view)
    WebView webView;

    @ViewById(R.id.login_username)
    EditText userNameView;
    @ViewById(R.id.login_password)
    EditText passwordView;
    @ViewById(R.id.rememberMe)
    CheckBox rememberMe;
    @ViewById(R.id.forgot_password)
    TextView forgotPassword;

    @Extra
    String username;
    @Extra
    String password;
    @Extra
    String url;

    @Bean
    RestApi restApi;

    @Bean
    LayoutManager layoutManager;

    AccountManager accountManager =
            (AccountManager) App.getInstance().getSystemService(ACCOUNT_SERVICE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @AfterViews
    void initialize() {
        if (url.equalsIgnoreCase(getString(R.string.logout_url))) {
            container.setVisibility(View.GONE);
            openBackgroundLogoutWebView();
            return;
        }

        passwordView.setTypeface(Typeface.DEFAULT);
        passwordView.setTransformationMethod(new PasswordTransformationMethod());

        passwordView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    loginClick();
                    return true;
                }
                return false;
            }
        });

        checkAccount(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkAccount(boolean initialCheck) {
        if (username != null) {
            Account newAccount = new Account(username, ACCOUNT_TYPE);

            accountManager.addAccountExplicitly(newAccount, password, null);

            if (initialCheck) {
                container.setVisibility(View.GONE);
                openBackgroundLoginWebView();
            }
        }
    }


    @Click(R.id.login_button)
    protected void loginClick() {
        if (!userNameView.getText().toString().isEmpty() &&
                !passwordView.getText().toString().isEmpty()) {
            username = userNameView.getText().toString();
            password = passwordView.getText().toString();

            openBackgroundLoginWebView();
        } else {
            showShortToast(getResources().getString(R.string.form_error));
        }
    }


    @Click(R.id.forgot_password)
    protected void forgotPasswordClick() {
        PortletWebViewActivity_
                .intent(LoginActivity.this)
                .url(App.getInstance().getResources().getString(R.string.forgot_password_url))
                .start();
    }

    protected void openBackgroundLoginWebView() {
        getActionBar().setDisplayHomeAsUpEnabled(false);
        showSpinner();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            private boolean initialLoginRequest = true;

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (url.equalsIgnoreCase(getString(R.string.home_page))) {
                    if (rememberMe.isChecked()) {
                        checkAccount(false);
                    }
                    getLoggedInFeed();
                    App.setIsAuth(true);
                }
                super.onPageStarted(view, url, favicon);

            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (initialLoginRequest) {
                    initialLoginRequest = false;
                    view.loadUrl("javascript:$('#username').val('" + username + "');");
                    view.loadUrl("javascript:$('#password').val('" + password + "');");
                    view.loadUrl("javascript:$('.btn-submit').click();");
                } else {
                    if (url.equalsIgnoreCase(getString(R.string.login_url))) {
                        dismissSpinner();
                        showLongToast(getResources().getString(R.string.information_error));
                    }
                }
                super.onPageFinished(view, url);
            }
        });
        webView.loadUrl(url);
    }

    protected void openBackgroundLogoutWebView() {
        showSpinner();
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            private boolean receivedError = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (receivedError) {
                    showLongToast(getString(R.string.error_network_connection));
                    super.onPageFinished(view, url);
                    finish();

                    dismissSpinner();
                    return;
                }

                // logged out successfully
                restApi.setCookie("");
                removeAccount();
                App.setIsAuth(false);
                getFeed();
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                receivedError = true;
            }
        });
        webView.loadUrl(url);
    }

    private void removeAccount() {
        if (accountManager.getAccountsByType(ACCOUNT_TYPE).length != 0) {
            accountManager.removeAccount(
                    accountManager.getAccountsByType(ACCOUNT_TYPE)[0], null, null);
        }
    }

    private void getLoggedInFeed() {
        String cookie = CookieManager.getInstance().getCookie(getString(R.string.base_url));

        if (cookie != null) {
            String[] temp = cookie.split(" ");
            for (String key : temp) {
                if (key.contains(AppConstants.JSESSIONID)) {
                    restApi.setCookie(key);
                    break;
                }
            }
        }
        getFeed();
    }

    private void getFeed() {
        restApi.getMainFeed(new UmobileRestCallback<String>() {

            @Override
            public void onBegin() {
                super.onBegin();
            }

            @Override
            public void onError(Exception e, String responseBody) {
                Logger.e(TAG, responseBody, e);

            }

            @Override
            public void onSuccess(String response) {
                Gson g = new GsonBuilder()
                        .registerTypeAdapter(Layout.class, new LayoutDeserializer())
                        .create();

                Layout layout = g.fromJson(response, Layout.class);
                layoutManager.setLayout(layout);
                HomePage_
                        .intent(LoginActivity.this)
                        .flags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .start();
                finish();
            }

            @Override
            public void onFinish() {
                super.onFinish();
                dismissSpinner();
            }
        });
    }

}
