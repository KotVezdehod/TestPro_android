package ru.companyname.testpro;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class BroadcastReceiverJavaClass implements Runnable
{
    static native void OnBroadcastCatched(long pObject, String str_out);
    static native void OnHttpServerServ(long pObject, String str_url);;


    private long m_V8Object; // 1C application context
    private Activity m_Activity; // custom activity of 1C:Enterprise
    private BroadcastReceiver m_Receiver;
    private String bk_filter;
    JSONArray bk_filters;
    public BroadcastReceiverJavaClass(Activity activity, long v8Object)
    {
        m_Activity = activity;
        m_V8Object = v8Object;
    }
    public void run(){System.loadLibrary("ru_companyname_testpro");}
    public void show()
    {
        m_Activity.runOnUiThread(this);
    }

    MyHTTPD http_server;
    int HTT_PORT = 8765;

    public void start(String in_bk_filters)
    {
        String in_str_intent_filters = in_bk_filters;
        bk_filter = in_str_intent_filters;
        IntentFilter filter = new IntentFilter();

        try
        {
            bk_filters = new JSONArray(bk_filter);
            String str_current_filter = "";
            JSONObject jo;
            for(int i=0;i<bk_filters.length();i++)
            {
                jo = (JSONObject)bk_filters.getJSONObject(i);
                str_current_filter = jo.getString("filter");
                filter.addAction(str_current_filter);
            }

        }
        catch(Exception ex)
        {return;}

        if (m_Receiver==null)
        {
            m_Receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    String intent_action;
                    String extra = "";
                    String extra_key = "";
                    byte[] b0;

                    for(int i=0;i<bk_filters.length();i++)
                    {
                        try
                        {

                            intent_action = bk_filters.getJSONObject(i).getString("filter");
                            if (intent_action.equals(action))
                            {
                                extra_key = bk_filters.getJSONObject(i).getString("extra");
                                if (extra_key!="")
                                {
                                    extra = intent.getStringExtra(extra_key);
                                    if (extra==null)
                                    {
                                        b0 = intent.getByteArrayExtra(extra_key);
                                        if (b0!=null)
                                        {
                                            extra = new String(b0);
                                        }
                                    }
                                    if (extra==null || extra=="")
                                    {
                                        extra = "Доп. данные с ключом: '" + extra_key + "' не содержат данных!";
                                    }

                                    cl_bk_result loc_ob = new cl_bk_result();
                                    loc_ob.action = intent_action;
                                    loc_ob.key = extra_key;
                                    loc_ob.Data = extra;

                                    Gson gson = new Gson();

                                    OnBroadcastCatched(m_V8Object, gson.toJson(loc_ob));
                                }
                                else
                                {
                                    OnBroadcastCatched(m_V8Object, "ok_en");
                                }
                                break;
                            }
                        }
                        catch(Exception ex) {}
                    }
                }
            };

            m_Activity.registerReceiver(m_Receiver, filter);
        }
    }

    public void stop()
    {
        if (m_Receiver != null)
        {
            m_Activity.unregisterReceiver(m_Receiver);
            m_Receiver = null;
        }
    }


    //================ HTTP
    public String StartHTTP(int in_port)
    {
        HTT_PORT = in_port;
        if (http_server!=null)
        {
            try
            {
                http_server.closeAllConnections();
                http_server.stop();
                http_server = null;
            }
            catch(Exception e)
            {
                return e.getMessage();
            }
        }

        try
        {
            http_server = new MyHTTPD();
        }
        catch(IOException e)
        {
            return e.getMessage();
        }

        try {
            http_server.start();
        } catch (Exception e) {
            return e.getMessage();
        }
        return "ok_en";

    }

    public String StopHTTP()
    {

        if (http_server!=null)
        {
            try
            {
                http_server.closeAllConnections();
                http_server.stop();
                http_server = null;
            }
            catch(Exception e)
            {
                return e.getMessage();
            }
        }

        return "ok_en";
    }


    public class MyHTTPD extends NanoHTTPD {
        //public static final int PORT = 8765;

        public MyHTTPD() throws IOException {
            super(HTT_PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {

            Map<String, String> files = new HashMap<String, String>();
            Method method = session.getMethod();

            if (Method.PUT.equals(method) || Method.POST.equals(method)) {
                try {
                    session.parseBody(files);
                } catch (IOException ioe) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                } catch (ResponseException re) {
                    return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                }
            }

            for (Map.Entry<String, String> entry: files.entrySet())
            {
                OnHttpServerServ(m_V8Object, entry.getValue());
                String response = "ok";
                return newFixedLengthResponse(response);
            }

           /* // get the POST body
            String postBody = session.getQueryParameterString();
            // or you can access the POST request's parameters
            String postParameter = session.getParms().get("parameter");

            return new Response(postBody); // Or postParameter.*/

            return  null;
        }
    }

    class cl_bk_result
    {
        String action = "";
        String key = "";
        String Data = "";
    }
}
