package com.example.winwinwin;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

public class MainActivity extends Activity {

    // GeckoRuntime must be a process-wide singleton
    private static GeckoRuntime sRuntime;
    private GeckoSession session;
    private boolean canGoBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // GeckoView uses Firefox engine — full Web Speech API / TTS support
        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this,
                new GeckoRuntimeSettings.Builder()
                    .javaScriptEnabled(true)
                    .remoteDebuggingEnabled(false)
                    .build());
        }

        session = new GeckoSession(
            new GeckoSessionSettings.Builder()
                .usePrivateMode(false)
                .suspendMediaWhenInactive(false)
                .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
                .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
                .build()
        );

        // JS injection via UserContentController (GeckoView 128+ API)
        GeckoSession.UserContentController ucc = session.getUserContentController();
        // CSS selector blocker — runs on every page via MutationObserver
        ucc.addUserScript(new GeckoSession.UserScript.Builder()
            .source("(function(){var S='';function h(){if(!S)return;try{document.querySelectorAll(S).forEach(function(el){if(el&&el.style)el.style.setProperty('display','none','important')});}catch(e){}}new MutationObserver(h).observe(document.documentElement,{childList:true,subtree:true});h();})();")
            .injectAt(GeckoSession.UserScript.USER_SCRIPT_INJECT_AT_END)
            .build());

        // Back navigation tracking + external link interception
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onCanGoBack(GeckoSession s, boolean value) {
                canGoBack = value;
            }

            @Override
            public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession s,
                    GeckoSession.NavigationDelegate.LoadRequest req) {
                if (req.uri.startsWith("intent:") || req.uri.startsWith("market:")) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(req.uri)));
                    } catch (ActivityNotFoundException e) { /* ignore */ }
                    return GeckoResult.fromValue(AllowOrDeny.DENY);
                }
                return null;
            }
        });

        GeckoView geckoView = new GeckoView(this);
        setContentView(geckoView);
        session.open(sRuntime);
        geckoView.setSession(session);
        session.loadUri("https://link.prod.sekai.chat/3HIL4M");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (canGoBack) {
                session.goBack(false);
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (session != null) session.close();
    }
}
