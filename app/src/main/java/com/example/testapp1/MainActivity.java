package com.example.testapp1;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView; //카카오 맵뷰 임포트

import android.util.Log;
import android.view.ViewGroup; //뷰 그룹 임포트
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapView mapView = new MapView(this);

        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view); //뷰그룹 사용
        mapViewContainer.addView(mapView);

        //apiparse (테스트)
        ApiParse apiData = new ApiParse();
        ArrayList<CoorData> dataArr = apiData.getData();

        // Polyline 좌표 지정.
        for(int i=0; i<dataArr.size(); i++) { // 매 CoorData마다 폴리라인 객체 생성
            MapPolyline polyline = new MapPolyline();
            polyline.setLineColor(Color.argb(128, 255, 51, 0));
            polyline.setTag(i);
            for(int x=0; x<dataArr.get(i).Coords.length/2; x++) {
                polyline.addPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(dataArr.get(i).Coords[x*2+1]), Double.parseDouble(dataArr.get(i).Coords[x*2])));
            }
            mapView.addPolyline(polyline); // 폴리라인 객체 지도에 올리기
        }

        // 지도뷰의 중심좌표와 줌레벨을 Polyline이 모두 나오도록 조정.
        mapView.fitMapViewAreaToShowAllPolylines();

        //텍스트뷰 (테스트)
        TextView textView1 = (TextView) findViewById(R.id.text1) ;
        textView1.setText(dataArr.get(2).Coords[1]);

    }

}

class CoorData {
    String[] Coords;
}

class ApiParse {
    public ArrayList<CoorData> getData() {
        //return data 부분
        ArrayList<CoorData> dataArr = new ArrayList<CoorData>();
        Thread t = new Thread() {
            @Override
            public  void run() {
                try {
                    //요청 Url
                    String fullurl = "https://api.vworld.kr/req/data?service=data&request=GetFeature&key=F931BD24-945F-3AA9-8CB7-853B5D40C5A8&format=xml&data=LT_L_FRSTCLIMB&attrFilter=emdCd:=:11620103";
                    URL url = new URL(fullurl);
                    InputStream is = url.openStream();

                    //xmlParser 생성
                    XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = xmlFactory.newPullParser();
                    parser.setInput(is,"utf-8");

                    //xml과 관련된 변수들
                    boolean isCoords = false;
                    String Coords = "";

                    // 파싱 시작
                    while(parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                        int type = parser.getEventType();
                        CoorData data = new CoorData();

                        //태그 검사(태그가 gml:posList인 경우 찾기)
                        if(type == XmlPullParser.START_TAG) {
                            if (parser.getName().equals("gml:posList")) {
                              isCoords = true;
                            }
                        }
                        //텍스트 확인 (Coords에 텍스트 임시 저장)
                        else if(type == XmlPullParser.TEXT) {
                            if(isCoords) {
                                Coords = parser.getText();
                                isCoords = false;
                            }
                        }
                        // 데이터 추가 (Coords데이터 공백으로 스플릿하여 저장)
                        else if(type == XmlPullParser.END_TAG && parser.getName().equals("gml:posList")) {
                            data.Coords = Coords.split(" ");

                            dataArr.add(data);
                        }

                        type = parser.next();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return dataArr;
    }
}