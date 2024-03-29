package com.dizach.androidbrowser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ProgressBar progressBar;
    ImageView iconImageView;
    WebView webView;
    LinearLayout linearLayoutProgressIcon;
    EditText URLBar;
    Button goButton;
    String currentURL;
    String homePage = "https://www.google.com/";
    String bookmarksString;
    ArrayList<String> bookmarks;
    ArrayList<String> urlHistory;
    WebBackForwardList history;
    Activity self = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set content view
        setContentView(R.layout.activity_main);

        // instantiate layout items
        progressBar = findViewById(R.id.progressBar);
        iconImageView = findViewById(R.id.iconImageView);
        webView = findViewById(R.id.webViewMain);
        linearLayoutProgressIcon = findViewById(R.id.linearLayoutProgressIcon);
        URLBar = findViewById(R.id.editTextURL);
        goButton = findViewById(R.id.goButton);

        progressBar.setMax(100);

        // obtain saved data from shared preferences
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        // load home page
        String defaultHomePage = getResources().getString(R.string.default_home_page);
        homePage = sharedPref.getString(getString(R.string.user_set_home_page_key), defaultHomePage);
        goToURL(homePage);

        // load bookmarks string (URLs separated by ',')
        String defaultBookmarks = getResources().getString(R.string.default_user_bookmarks);
        bookmarksString = sharedPref.getString(getString(R.string.user_bookmarks_key), defaultBookmarks);
        parseBookmarks(bookmarksString); // parse bookmarks

        // enable javascript
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                // show progress bar while page is loading
                linearLayoutProgressIcon.setVisibility(View.VISIBLE);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // hide progress bar once loading is finished
                linearLayoutProgressIcon.setVisibility(View.GONE);

                // update address in URL bar
                URLBar.setText(url);
                currentURL = url;

                // hide keyboard
                hideKeyboard(self);
            }
        });

        // use webchromeclient to add actions based on page loading hooks
        webView.setWebChromeClient(new WebChromeClient(){

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // update progress bar
                progressBar.setProgress(newProgress);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                // display page title
                getSupportActionBar().setTitle(title);
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
                // display page icon
                iconImageView.setImageBitmap(icon);
            }
        });

        // enable file download
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

                DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(url));
                downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                downloadManager.enqueue(downloadRequest);

                Toast.makeText(MainActivity.this, "File is downloading", Toast.LENGTH_SHORT).show();
            }
        });

        // set go button event listener
        goButton.setOnClickListener((View v) -> {
            String toURL = URLBar.getText().toString();
            goToURL(toURL);
        });
        // want to go to page if user hits enter on onscreen keyboard
        URLBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    String toURL = URLBar.getText().toString();
                    goToURL(toURL);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    public void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void parseBookmarks(String bookmarksString) {
        bookmarks = new ArrayList<String>(Arrays.asList(bookmarksString.split(",")));
    }

    public void goToURL(String toURL) {
        // check for https:// prefix and add if missing
        if (!toURL.startsWith("https://")) toURL = "https://" + toURL;
        webView.loadUrl(toURL);
    }

    // "inflate" menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // wire up the menu buttons
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_back:
                onBackPressed();
                break;
            case R.id.menu_forward:
                onForwardPressed();
                break;
            case R.id.menu_refresh:
                webView.reload();
                break;
            case R.id.menu_go_to_home_page:
                goToURL(homePage);
                break;
            case R.id.menu_set_home_page:
                setHomePage();
                break;
            case R.id.menu_add_to_bookmarks:
                addToBookmarks(currentURL);
                break;
            case R.id.menu_view_bookmarks:
                displayBookmarks();
                break;
            case R.id.menu_view_history:
                showHistory();
                break;
            case R.id.menu_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, currentURL);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Copied URL");
                startActivity(Intent.createChooser(shareIntent, "Share URL"));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void displayBookmarks() {
        BookmarksListFragment BookmarksListFragment = new BookmarksListFragment(bookmarks);
        BookmarksListFragment.show(getSupportFragmentManager(), "BOOKMARKS_LIST_FRAGMENT_TAG");
    }

    public void showHistory() {
        // get history
        WebBackForwardList currentList = webView.copyBackForwardList();
        history = currentList;
        urlHistory = new ArrayList<String>();
        int currentSize = currentList.getSize();
        for(int i = 0; i < currentSize; i++) {
            WebHistoryItem item = currentList.getItemAtIndex(i);
            String url = item.getUrl();
            urlHistory.add(url);
        }
        // show history
        HistoryListFragment HistoryListFragment = new HistoryListFragment(urlHistory);
        HistoryListFragment.show(getSupportFragmentManager(), "HISTORY_LIST_FRAGMENT_TAG");
    }

    private void setHomePage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Home Page URL");

        // Set input with current URL
        EditText input = new EditText(this);
        input.setText(currentURL);

        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the okay/cancel buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                homePage = input.getText().toString();

                // save home page to sharedpreferences
                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.user_set_home_page_key), homePage);
                editor.apply();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void addToBookmarks(String url) {
        if (bookmarks.contains(url)) {
            Toast.makeText(this, "Bookmark already added", Toast.LENGTH_SHORT).show();
            return;
        }

        // add to bookmarks
        bookmarks.add(url);
        setBookmarksString();
        Toast.makeText(this, "Added to bookmarks", Toast.LENGTH_SHORT).show();

        // save to shared preferences
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.user_bookmarks_key), bookmarksString);
        editor.apply();
    }

    public void removeFromBookmarks(String url) {
        if (!bookmarks.contains(url)) {
            Toast.makeText(this, "Bookmark not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // remove from bookmarks
        bookmarks.remove(url);
        setBookmarksString();
        Toast.makeText(this, "Removed from bookmarks", Toast.LENGTH_SHORT).show();

        // save to shared preferences
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.user_bookmarks_key), bookmarksString);
        editor.apply();
    }

    private void setBookmarksString() {
        String newBookmarksString = "";
        for (String bookmark : bookmarks) {
            newBookmarksString += bookmark + ",";
        }
        bookmarksString = newBookmarksString;
    }

    private void onForwardPressed() {
        if (webView.canGoForward()) {
            webView.goForward();
        } else {
            Toast.makeText(this, "Can't go further!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // close app if back button is pressed but there's no previous page in history
            finish();
        }
    }
}